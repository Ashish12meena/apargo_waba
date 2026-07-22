package com.apargo.waba.api.request;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * One item inside the top-level {@code entry[]} array of a Meta webhook
 * notification.
 *
 * <p>
 * A single HTTP POST from Meta can batch changes for more than one WABA,
 * so this is always a list at the top level - never assume {@code entry}
 * has exactly one element.
 *
 * <pre>
 * {
 *   "object": "whatsapp_business_account",
 *   "entry": [
 *     {
 *       "id": "&lt;WABA_ID&gt;",
 *       "changes": [ { "field": "...", "value": { ... } } ]
 *     }
 *   ]
 * }
 * </pre>
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookEntry {

    /**
     * Meta's WABA ID this entry belongs to.
     * <p>
     * Maps to {@code WabaAccount.wabaId} - use this to resolve which
     * organization/tenant the event belongs to before doing anything else.
     */
    private String id;

    /**
     * The list of changes reported for this WABA in this notification.
     */
    private List<WebhookChange> changes;

}
