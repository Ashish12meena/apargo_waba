package com.apargo.waba.api.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One item inside {@code entry[].changes[]}.
 *
 * <h2>Why {@code value} is a raw {@link JsonNode} and not a typed object</h2>
 *
 * Meta reuses this same envelope for a large number of unrelated event
 * types, distinguished only by {@link #field}. Known values documented by
 * Meta include (non-exhaustive, and Meta adds new ones without notice):
 *
 * <ul>
 *     <li>{@code messages} - inbound customer messages / message status
 *         updates (sent, delivered, read, failed)</li>
 *     <li>{@code message_template_status_update} - template approval /
 *         rejection / pause / disable events</li>
 *     <li>{@code message_template_quality_update} - template quality score
 *         changes</li>
 *     <li>{@code account_update} - WABA-level status/ban/restriction
 *         changes</li>
 *     <li>{@code account_alerts} - portfolio-level alerts</li>
 *     <li>{@code phone_number_quality_update} - quality rating changes for
 *         a phone number</li>
 *     <li>{@code phone_number_name_update} - display name approval status
 *         changes</li>
 *     <li>{@code capability_update} - messaging limit tier / throughput
 *         tier changes</li>
 *     <li>{@code security} - two-step verification code changes</li>
 * </ul>
 *
 * Each of these has a completely different {@code value} shape. Trying to
 * flatten all of them into one Java class produces either a huge class full
 * of always-null fields, or silent data loss on fields we didn't anticipate.
 *
 * Instead, the controller/service boundary is kept generic here. The
 * service layer is expected to switch on {@link #field} and deserialize
 * {@link #value} into a specific value DTO
 * (e.g. {@code WebhookMessageValue}, {@code WebhookStatusValue}) using
 * {@code objectMapper.treeToValue(change.getValue(), TargetClass.class)}.
 *
 * This also matches the "store Meta-controlled data with forward
 * compatibility in mind" principle already used elsewhere in this service
 * (see {@code Meta_WhatsApp_Architecture_Notes.md}) - an unrecognized
 * {@code field} value should never break deserialization of the envelope.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WebhookChange {

    /**
     * Identifies which kind of event this is.
     * <p>
     * Kept as a raw {@link String} (not an enum) deliberately - Meta can
     * introduce new field values at any time, and an enum would force a
     * deployment just to avoid failing deserialization on an unknown value.
     */
    private String field;

    /**
     * The event payload. Shape depends entirely on {@link #field}.
     * <p>
     * Deliberately untyped at this layer - see class-level Javadoc.
     */
    private JsonNode value;

}
