package com.apargo.waba.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Top-level request body sent by Meta on every WhatsApp Cloud API webhook
 * notification (POST).
 *
 * <p>
 * Reference: <a href="https://developers.facebook.com/docs/whatsapp/cloud-api/guides/set-up-webhooks/">
 * Set up webhooks - Cloud API guide</a>
 *
 * <pre>
 * {
 *   "object": "whatsapp_business_account",
 *   "entry": [
 *     {
 *       "id": "&lt;WABA_ID&gt;",
 *       "changes": [
 *         {
 *           "field": "messages",
 *           "value": { "messaging_product": "whatsapp", "...": "..." }
 *         }
 *       ]
 *     }
 *   ]
 * }
 * </pre>
 *
 * This class intentionally mirrors only the stable envelope. The variable
 * part ({@code changes[].value}) is deserialized generically -
 * see {@link WebhookChange}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhatsAppWebhookPayload {

    /**
     * Always {@code "whatsapp_business_account"} for WABA-scoped webhooks.
     * Kept as a raw String (rather than validated/enum) so that an
     * unexpected value never breaks parsing - the service layer can decide
     * whether to reject it.
     */
    private String object;

    /**
     * One or more WABAs whose events are batched into this notification.
     */
    private List<WebhookEntry> entry;

}