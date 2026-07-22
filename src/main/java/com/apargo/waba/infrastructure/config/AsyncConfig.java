package com.apargo.waba.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Enables and configures asynchronous method execution for this service.
 *
 * <h2>Why this exists</h2>
 *
 * {@code WabaWebhookController} must return a fast {@code 200 OK} to Meta -
 * Meta retries non-200 responses for up to 7 days, and slow synchronous
 * handling on the request thread risks timeouts under load. The
 * {@code @Async}-annotated {@code processWebhookEvent} method in
 * {@code WabaWebhookServiceImpl} runs on the dedicated executor defined
 * here instead of the servlet request thread.
 *
 * <p>Pool sizing is entirely driven by {@link WebhookProperties} - nothing
 * here is hardcoded, so it can be tuned per environment via
 * {@code application.yaml} without a code change.
 */
@Slf4j
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfig implements AsyncConfigurer {

    private static final String WEBHOOK_EXECUTOR_BEAN_NAME = "webhookTaskExecutor";

    private final WebhookProperties webhookProperties;

    /**
     * Dedicated executor for webhook event processing.
     * <p>
     * Referenced explicitly via {@code @Async("webhookTaskExecutor")} on
     * the processing method rather than relying on
     * {@link #getAsyncExecutor()} as a service-wide default, so future
     * async work (e.g. Graph API sync jobs) can get its own differently
     * tuned pool without affecting webhook throughput.
     */
    @Bean(name = WEBHOOK_EXECUTOR_BEAN_NAME)
    public Executor webhookTaskExecutor() {
        WebhookProperties.Async config = webhookProperties.getAsync();

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setTaskDecorator(mdcPropagatingTaskDecorator());
        executor.setRejectedExecutionHandler(loggingRejectionHandler());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(20);
        executor.initialize();
        return executor;
    }

    /**
     * Default executor for any other {@code @Async} method that doesn't
     * name a specific bean. Reuses the same webhook pool for now since it's
     * the only async workload in the service; revisit if a second async
     * use case with different load characteristics is added.
     */
    @Override
    public Executor getAsyncExecutor() {
        return webhookTaskExecutor();
    }

    /**
     * Ensures an uncaught exception in a fire-and-forget {@code void}
     * {@code @Async} method (like {@code processWebhookEvent}) is logged
     * instead of silently disappearing - Spring does not propagate
     * exceptions from async void methods back to any caller.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, Method method, Object... params) ->
                log.error("Uncaught exception in async method [{}] with args {}",
                        method.getName(), params, ex);
    }

    /**
     * Propagates the logging MDC (correlation/trace IDs) from the servlet
     * request thread onto the async worker thread, so log lines for a
     * given webhook delivery can still be correlated end-to-end even
     * though processing happens off-thread.
     */
    private TaskDecorator mdcPropagatingTaskDecorator() {
        return runnable -> {
            var contextMap = org.slf4j.MDC.getCopyOfContextMap();
            return () -> {
                try {
                    if (contextMap != null) {
                        org.slf4j.MDC.setContextMap(contextMap);
                    }
                    runnable.run();
                } finally {
                    org.slf4j.MDC.clear();
                }
            };
        };
    }

    private RejectedExecutionHandler loggingRejectionHandler() {
        return (Runnable task, ThreadPoolExecutor executor) ->
                log.error("Webhook task rejected — executor pool exhausted "
                                + "(active={}, queueSize={}, poolSize={}). "
                                + "Consider raising webhook.async.max-pool-size "
                                + "or webhook.async.queue-capacity.",
                        executor.getActiveCount(), executor.getQueue().size(), executor.getPoolSize());
    }
}