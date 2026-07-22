# Onboarding (Embedded Signup) — Full Request Flow

Ye document `POST /api/v1/onboarding` se lekar `OnboardingTask` ke `COMPLETED` hone tak, request kahan-kahan se hoke guzarti hai — layer by layer, step by step.

---

## 1. High-Level Layer Flow

```
┌─────────────┐
│   Client     │  (Meta Embedded Signup se oauthCode mila hai frontend pe)
└──────┬───────┘
       │ POST /api/v1/onboarding
       │ { organizationId, projectId, oauthCode, idempotencyKey }
       ▼
┌─────────────────────────┐
│  OnboardingController    │  api/v1/  — sirf validation + delegation, ZERO business logic
└──────┬────────────────────┘
       │ calls interface
       ▼
┌─────────────────────────┐
│  OnboardingUsecase        │  application/port/in/  — contract, controller ko implementation nahi pata
└──────┬────────────────────┘
       │ implemented by
       ▼
┌─────────────────────────┐
│  OnboardingServiceImpl    │  application/usecase/
│  - idempotency check      │
│  - task create (PENDING)  │
│  - state transitions      │
└──────┬────────────────────┘
       │ (1) save via port          │ (2) fire-and-forget
       ▼                            ▼
┌──────────────────────┐   ┌──────────────────────────────┐
│ OnboardingTaskRepo     │   │  OnboardingWorkflowExecutor    │  @Async — separate bean
│ Port → JPA Adapter     │   │  (application/usecase/)         │  (self-invocation se bachne ke liye
│ → MySQL                │   │                                  │   OnboardingServiceImpl se alag)
└──────────────────────┘   └──────────┬───────────────────────┘
                                       │ runs on webhookTaskExecutor thread pool
                                       │ loops through OnboardingStep 1→12
                                       ▼
                            ┌──────────────────────────┐
                            │   MetaGraphApiPort         │  application/port/out/
                            └──────────┬─────────────────┘
                                       │ implemented by
                                       ▼
                            ┌──────────────────────────┐
                            │  MetaGraphApiAdapter        │  infrastructure/client/
                            │  (Spring RestClient)         │
                            └──────────┬─────────────────┘
                                       │ HTTPS
                                       ▼
                            ┌──────────────────────────┐
                            │   graph.facebook.com        │  Meta Graph API
                            └──────────────────────────┘
```

**Kya turant client ko wapas milta hai:** Controller **workflow ke complete hone ka wait nahi karta**. `startOnboarding()` sirf task ko `PENDING`/`PROCESSING` status mein banata hai, `OnboardingWorkflowExecutor.run()` ko async trigger karta hai, aur turant `201 Created` + task snapshot return kar deta hai. Client ko progress dekhne ke liye `GET /api/v1/onboarding/{taskId}` poll karna hota hai.

---

## 2. Request Sequence (turn-by-turn)

```
Client                Controller           ServiceImpl         WorkflowExecutor        Meta Graph API        Database
  │                       │                      │                      │                      │                  │
  │  POST /onboarding     │                      │                      │                      │                  │
  ├──────────────────────►│                      │                      │                      │                  │
  │                       │ @Valid check         │                      │                      │                  │
  │                       ├─────────────────────►│                      │                      │                  │
  │                       │                      │ findByIdempotencyKey │                      │                  │
  │                       │                      ├──────────────────────┼──────────────────────┼─────────────────►│
  │                       │                      │◄─────────────────────┼──────────────────────┼──────────────────┤
  │                       │                      │  [not found]         │                      │                  │
  │                       │                      │ save(task=PENDING)   │                      │                  │
  │                       │                      ├──────────────────────┼──────────────────────┼─────────────────►│
  │                       │                      │                      │                      │                  │
  │                       │                      │ workflowExecutor.run(taskId)  [ASYNC — returns immediately]     │
  │                       │                      ├─────────────────────►│                      │                  │
  │                       │◄─────────────────────┤                      │                      │                  │
  │  201 Created          │                      │                      │                      │                  │
  │◄──────────────────────┤                      │                      │                      │                  │
  │  {status: PROCESSING} │                      │                      │                      │                  │
  │                       │                      │                      │                      │                  │
  .     (client ka turn khatam ho gaya; niche wala sab background thread pe chalta hai)         │                  │
  │                       │                      │                      │                      │                  │
  │                       │                      │            task.start(TOKEN_EXCHANGE)        │                  │
  │                       │                      │                      ├──────────────────────┼─────────────────►│
  │                       │                      │                      │ POST /oauth/access_token (form)         │
  │                       │                      │                      ├─────────────────────►│                  │
  │                       │                      │                      │◄─────────────────────┤                  │
  │                       │                      │                      │ {access_token,...}    │                  │
  │                       │                      │                      │ encrypt + save         │                  │
  │                       │                      │                      ├──────────────────────┼─────────────────►│
  │                       │                      │                      │ moveToStep(TOKEN_EXTENSION)              │
  │                       │                      │                      │        ⋮ (loop repeats for each step)   │
  │                       │                      │                      │                      │                  │
  │  GET /onboarding/{id} │                      │                      │                      │                  │
  ├──────────────────────►│─────────────────────►│ findById(id)         │                      │                  │
  │                       │                      ├──────────────────────┼──────────────────────┼─────────────────►│
  │  200 {status, step}   │                      │◄─────────────────────┼──────────────────────┼──────────────────┤
  │◄──────────────────────┤                      │                      │                      │                  │
```

---

## 3. Step-by-Step Breakdown (`OnboardingWorkflowExecutor` loop)

Har step **complete hone ke baad turant DB mein save hota hai** (checkpointing) — crash ho jaaye to `retryTask()` last successful step se resume karta hai.

| # | Step | Graph API Call | Kya update hota hai | Agla step |
|---|---|---|---|---|
| 1 | `TOKEN_EXCHANGE` | `POST /oauth/access_token` (form: client_id, client_secret, code, redirect_uri) | `task.encryptedAccessToken`, `task.tokenExpiresIn` | `TOKEN_EXTENSION` |
| 2 | `TOKEN_EXTENSION` | `POST /oauth/access_token` (grant_type=fb_exchange_token) | Long-lived token overwrite | `SCOPE_VERIFICATION` |
| 3 | `SCOPE_VERIFICATION` | `GET /debug_token?input_token=` (app token se auth) | — (sirf validate, fail ho to task.fail()) | `BUSINESS_MANAGER_RESOLUTION` |
| 4 | `BUSINESS_MANAGER_RESOLUTION` | `GET /me/businesses` | `task.resolvedBusinessManagerId` | `WABA_RESOLUTION` |
| 5 | `WABA_RESOLUTION` | `GET /{business-id}/owned_whatsapp_business_accounts` | `task.resolvedWabaId` | `PHONE_NUMBER_RESOLUTION` |
| 6 | `PHONE_NUMBER_RESOLUTION` | `GET /{waba-id}/phone_numbers` | `task.resolvedPhoneNumberId` | `CREDENTIAL_PERSISTENCE` |
| 7 | `CREDENTIAL_PERSISTENCE` | — (koi API call nahi) | **DB writes:** `MetaOAuthToken` upsert, `WabaAccount` create, `WabaPhoneNumber` create; `task.resultWabaAccountId` set | `WEBHOOK_SUBSCRIPTION` |
| 8 | `WEBHOOK_SUBSCRIPTION` | `POST /{waba-id}/subscribed_apps` | — | `PHONE_SYNC` |
| 9 | `PHONE_SYNC` | `GET /{waba-id}/phone_numbers` (dobara) | `WabaPhoneNumber` records upsert (display name, verified name) | `PHONE_REGISTRATION` |
| 10 | `PHONE_REGISTRATION` | ⚠️ **skipped** — PIN required, abhi API contract mein nahi | — | `SMB_SYNC` |
| 11 | `SMB_SYNC` | ⚠️ no-op — coexistence-only | — | `PHASE2_PROVISIONING` |
| 12 | `PHASE2_PROVISIONING` | ⚠️ no-op — optional System User upgrade | `task.status = COMPLETED` | — |

---

## 4. Baaki API Endpoints — Kaun Kya Karta Hai

```
GET  /api/v1/onboarding/{taskId}
  Client → Controller → ServiceImpl.getTask() → RepositoryPort.findById() → DB
  (koi async trigger nahi — sirf current state read)

GET  /api/v1/onboarding?organizationId=&status=
  Client → Controller → ServiceImpl.listTasks() → RepositoryPort.findByOrganizationId[AndStatus]() → DB

POST /api/v1/onboarding/{taskId}/retry
  Client → Controller → ServiceImpl.retryTask()
      → canRetry() check (status=FAILED && retryCount < maxRetries)
      → task.incrementRetry() + task.start(currentStep)   [resume, TOKEN_EXCHANGE se nahi]
      → save → WorkflowExecutor.run(taskId)  [same async loop, currentStep se aage]

POST /api/v1/onboarding/{taskId}/cancel
  Client → Controller → ServiceImpl.cancelTask()
      → isCompleted() check → task.cancel() → save
      (koi Graph API call nahi — sirf local state)

DELETE /api/v1/onboarding/{taskId}
  Client → Controller → ServiceImpl.deleteTask() → task.markDeleted() → save (soft delete)
```

---

## 5. Error Flow

```
Koi bhi step throw karta hai
        │
        ▼
MetaGraphApiException / generic Exception
        │
        ▼
OnboardingWorkflowExecutor.run() catch block
        │
        ▼
task.fail(errorMessage)  →  status = FAILED, errorMessage set, finishedAt set
        │
        ▼
save to DB  →  client ko GET se dikhega: {status: "FAILED", errorMessage: "..."}
        │
        ▼
Client calls POST /retry  →  agar canRetry() true hai, wapas loop shuru currentStep se
```

Alag se, controller-level errors (validation, not-found, invalid-state, upstream Meta failure) **`GlobalExceptionHandler`** se hokar guzarte hain — controller khud kabhi error response nahi banata:

```
Exception thrown anywhere in ServiceImpl
        │
        ▼
GlobalExceptionHandler (@RestControllerAdvice)
        │
        ├─ MethodArgumentNotValidException  → 400 + fieldErrors[]
        ├─ ResourceNotFoundException        → 404
        ├─ InvalidOnboardingStateException  → 409
        ├─ MetaGraphApiException            → 502
        ├─ TokenCipherException             → 500
        └─ Exception (fallback)             → 500
```

---

## 6. Idempotency Flow

```
POST /api/v1/onboarding  { idempotencyKey: "X" }
        │
        ▼
findByIdempotencyKey("X")
        │
   ┌────┴────┐
   │ Mila?    │
   └────┬────┘
   Yes  │   No
   ▼    │   ▼
Return  │  Naya OnboardingTask banao + async workflow trigger
existing│
task    │
(duplicate onboarding NAHI banta, chahe request 5 baar bhi aaye)
```

---

## 7. Thread Model

```
Servlet Request Thread (Tomcat)          webhookTaskExecutor Pool (async)
─────────────────────────────           ──────────────────────────────
Controller.startOnboarding()
  ServiceImpl.createAndStart()
    save(task)              ───┐
    workflowExecutor.run()  ───┼──► [hands off, DOES NOT WAIT]
  return 201                   │         WorkflowExecutor.run(taskId)
                                │           loop: step 1 → 2 → ... → 12
                                │           (MDC propagate ho jaata hai —
                                │            logs correlate karte hain)
Request thread FREE            │
(agla request serve karega)    │         DB writes yahin se hote hain
```

Isi wajah se **request thread kabhi 12-step Meta API chain ke liye block nahi hota** — Meta ke fast-200 expectation ka same principle jo webhook ke liye already tha, wahi yahan bhi follow hota hai.

---

## Reference

- Poora architecture + gaps analysis: `waba-service-architecture-and-api-plan.md`
- Implementation caveats: `OnboardingWorkflowExecutor.java` class-level Javadoc (per-step confidence level)