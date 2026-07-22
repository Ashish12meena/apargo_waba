# Meta WhatsApp Cloud API Architecture Notes

## Core hierarchy

``` text
Business Portfolio (Business Manager)
        │
        ├── System Users
        │      └── Permanent Access Token
        │
        └── WhatsApp Business Accounts (WABAs)
                └── Phone Numbers
```

## IDs

  --------------------------------------------------------------------------
  Name                       Meaning                 Stored as
  -------------------------- ----------------------- -----------------------
  Business Manager ID        Meta Business Portfolio `businessManagerId`

  WABA ID                    WhatsApp Business       `wabaId`
  (`whatsapp_business_id`)   Account identifier      

  Phone Number ID            Identifier used to send `phoneNumberId`
                             messages                

  System User ID             Identity that owns a    `systemUserId`
                             permanent token         
  --------------------------------------------------------------------------

## Tokens

### OAuth User Token

-   Created during Embedded Signup.
-   Short-lived (or exchanged for long-lived).
-   Belongs to a Meta user.
-   Used during onboarding.

### Permanent Token

-   Generated for a **System User**.
-   Belongs to the System User, **not** a WABA or phone number.
-   Can be used indefinitely (until revoked/rotated).
-   Recommended for production servers.

## Important misconception

A phone number **does not own a token**.

Request example:

``` http
POST /{PHONE_NUMBER_ID}/messages
Authorization: Bearer EAAG...
```

The endpoint uses the Phone Number ID, while the bearer token belongs to
a System User that has permission to access the Business Portfolio, WABA
and Phone Number.

## Database mapping

### meta_oauth_tokens

Stores credentials.

-   accessToken
-   tokenType (USER_TOKEN / SYSTEM_USER)
-   metaUserId
-   systemUserId
-   businessManagerId

### waba_accounts

Stores Meta WABAs.

-   wabaId
-   businessManagerId
-   messaging limit
-   review status
-   verification status

References `MetaOAuthToken`.

### waba_phone_numbers

Stores phone numbers belonging to a WABA.

-   phoneNumberId
-   displayPhoneNumber
-   quality
-   health
-   verification
-   throughput

### project_waba_assignments

Maps Projects to WABAs.

Allows multiple projects to share one WABA.

### onboarding_tasks

Tracks Embedded Signup workflow.

Stores: - status - currentStep - retryCount - idempotencyKey - temporary
onboarding data - workflow timestamps

## Workflow

``` text
User
   │
Embedded Signup
   │
OAuth Code
   │
Token Exchange
   │
Business Manager Resolution
   │
WABA Resolution
   │
Phone Resolution
   │
Persist Database
   │
Webhook Subscription
   │
Phone Sync
   │
Completed
```

## OnboardingStatus

-   PENDING
-   PROCESSING
-   COMPLETED
-   FAILED
-   CANCELLED

Represents the overall workflow state.

## OnboardingStep

Represents the current checkpoint.

Examples: - TOKEN_EXCHANGE - TOKEN_EXTENSION - SCOPE_VERIFICATION -
BUSINESS_MANAGER_RESOLUTION - WABA_RESOLUTION -
PHONE_NUMBER_RESOLUTION - CREDENTIAL_PERSISTENCE -
WEBHOOK_SUBSCRIPTION - PHONE_SYNC - PHONE_REGISTRATION - SMB_SYNC -
PHASE2_PROVISIONING

## Why both Status and Step?

Example:

Status = PROCESSING

Step = PHONE_REGISTRATION

If the server crashes, processing resumes from the recorded step instead
of repeating the whole onboarding process.

## Recommended schema relationships

``` text
MetaOAuthToken
        │
        ▼
WabaAccount
        │
        ▼
WabaPhoneNumber

Project
        │
        ▼
ProjectWabaAssignment
```

## Best practices

-   Store Meta-controlled statuses as VARCHAR in MySQL for forward
    compatibility.
-   Use Java enums in the application layer.
-   Store `completedSteps` as JSON.
-   Use soft deletes consistently.
-   Keep permanent tokens encrypted at rest.
-   Never store a token in a phone-number entity.
