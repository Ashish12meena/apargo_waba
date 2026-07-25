package com.apargo.waba.api.v1;

import com.apargo.waba.api.request.CreateWabaPhoneNumberRequest;
import com.apargo.waba.api.request.UpdateWabaPhoneNumberRequest;
import com.apargo.waba.api.response.WabaPhoneNumberResponse;
import com.apargo.waba.application.port.in.WabaPhoneNumberUsecase;
import com.apargo.waba.domain.enums.PhoneNumberStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * CRUD over {@link com.apargo.waba.domain.entity.WabaPhoneNumber}.
 * <p>
 * Contains no business logic — delegates immediately to
 * {@link WabaPhoneNumberUsecase}, per {@code docs/rules.md}.
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/waba-phone-numbers")
@RequiredArgsConstructor
@Tag(name = "WABA Phone Numbers", description = "CRUD for WhatsApp phone numbers registered under a WABA")
public class WabaPhoneNumberController {

    private final WabaPhoneNumberUsecase wabaPhoneNumberUsecase;

    @PostMapping
    @Operation(
            summary = "Register a phone number",
            description = "Registers a WhatsApp phone number under an existing WABA. "
                    + "The Meta Phone Number ID must be globally unique.")
    @ApiResponse(responseCode = "201", description = "Phone number registered")
    @ApiResponse(responseCode = "404", description = "Parent WABA account does not exist")
    @ApiResponse(responseCode = "409", description = "A phone number with the same Meta Phone Number ID already exists")
    public ResponseEntity<WabaPhoneNumberResponse> create(
            @Valid @RequestBody CreateWabaPhoneNumberRequest request,
            UriComponentsBuilder uriBuilder) {

        log.info("POST /api/v1/waba-phone-numbers wabaAccountId={}", request.getWabaAccountId());

        WabaPhoneNumberResponse created = wabaPhoneNumberUsecase.create(request);

        URI location = uriBuilder.path("/api/v1/waba-phone-numbers/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    @Operation(
            summary = "List phone numbers for a WABA",
            description = "Every phone number registered under the given WABA, optionally "
                    + "narrowed to a single operational status.")
    @ApiResponse(responseCode = "200", description = "Phone numbers returned (possibly empty)")
    @ApiResponse(responseCode = "404", description = "WABA account does not exist")
    public ResponseEntity<List<WabaPhoneNumberResponse>> list(
            @Parameter(description = "Internal id of the WABA to list phone numbers for")
            @RequestParam @NotNull @Positive Long wabaAccountId,

            @Parameter(description = "Optional filter on internal operational status")
            @RequestParam(required = false) PhoneNumberStatus status) {

        log.info("GET /api/v1/waba-phone-numbers wabaAccountId={} status={}", wabaAccountId, status);
        return ResponseEntity.ok(wabaPhoneNumberUsecase.listByWabaAccount(wabaAccountId, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a phone number by internal id")
    @ApiResponse(responseCode = "200", description = "Phone number found")
    @ApiResponse(responseCode = "404", description = "No phone number exists for the given id")
    public ResponseEntity<WabaPhoneNumberResponse> getById(
            @Parameter(description = "Internal database id", example = "17")
            @PathVariable @NotNull @Positive Long id) {

        log.info("GET /api/v1/waba-phone-numbers/{}", id);
        return ResponseEntity.ok(wabaPhoneNumberUsecase.getById(id));
    }

    @GetMapping("/phone-number-id/{whatsappPhoneNumberId}")
    @Operation(
            summary = "Get a phone number by Meta's Phone Number ID",
            description = "Resolves a phone using Meta's globally unique Phone Number ID — the "
                    + "identifier carried on inbound webhooks, not the display number.")
    @ApiResponse(responseCode = "200", description = "Phone number found")
    @ApiResponse(responseCode = "404", description = "No phone number exists for the given Meta Phone Number ID")
    public ResponseEntity<WabaPhoneNumberResponse> getByWhatsappPhoneNumberId(
            @Parameter(description = "Meta Phone Number ID", example = "108745612345678")
            @PathVariable @NotBlank String whatsappPhoneNumberId) {

        log.info("GET /api/v1/waba-phone-numbers/phone-number-id/{}", whatsappPhoneNumberId);
        return ResponseEntity.ok(
                wabaPhoneNumberUsecase.getByWhatsappPhoneNumberId(whatsappPhoneNumberId));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a phone number",
            description = "Partial update — null fields are left unchanged. The parent WABA and "
                    + "Meta Phone Number ID are immutable and cannot be changed here.")
    @ApiResponse(responseCode = "200", description = "Phone number updated")
    @ApiResponse(responseCode = "404", description = "No phone number exists for the given id")
    public ResponseEntity<WabaPhoneNumberResponse> update(
            @Parameter(description = "Internal database id", example = "17")
            @PathVariable @NotNull @Positive Long id,

            @Valid @RequestBody UpdateWabaPhoneNumberRequest request) {

        log.info("PUT /api/v1/waba-phone-numbers/{}", id);
        return ResponseEntity.ok(wabaPhoneNumberUsecase.update(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a phone number",
            description = "Soft delete — the row is retained with deleted_at stamped and becomes "
                    + "invisible to every subsequent read.")
    @ApiResponse(responseCode = "204", description = "Phone number deleted")
    @ApiResponse(responseCode = "404", description = "No phone number exists for the given id")
    public ResponseEntity<Void> delete(
            @Parameter(description = "Internal database id", example = "17")
            @PathVariable @NotNull @Positive Long id) {

        log.info("DELETE /api/v1/waba-phone-numbers/{}", id);
        wabaPhoneNumberUsecase.delete(id);
        return ResponseEntity.noContent().build();
    }
}