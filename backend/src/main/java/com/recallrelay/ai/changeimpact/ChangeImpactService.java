package com.recallrelay.ai.changeimpact;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.recallrelay.ai.promise.PromiseIntegrityResponse;
import com.recallrelay.ai.promise.PromiseIntegrityService;
import com.recallrelay.casework.RecallCaseQueryService;
import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ChangeImpactService {

    private static final String MODEL = "gpt-5.6";

    private final OpenAIClient openAIClient;
    private final RecallCaseQueryService recallCaseQueryService;
    private final PromiseIntegrityService promiseIntegrityService;

    public ChangeImpactService(
            OpenAIClient openAIClient,
            RecallCaseQueryService recallCaseQueryService,
            PromiseIntegrityService promiseIntegrityService
    ) {
        this.openAIClient = openAIClient;
        this.recallCaseQueryService = recallCaseQueryService;
        this.promiseIntegrityService = promiseIntegrityService;
    }

    public ChangeImpactResponse analyze(
            Long caseId,
            ChangeImpactRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Proposed change is required."
            );
        }

        RecallCaseDetailResponse caseData =
                recallCaseQueryService.getCaseDetail(caseId);

        RecallCaseDetailResponse.PartResponse part =
                caseData.parts() == null || caseData.parts().isEmpty()
                        ? null
                        : caseData.parts().get(0);

        if (part == null) {
            throw new IllegalStateException(
                    "Recall case has no part record."
            );
        }

        LocalDate beforeEta = part.eta();
        String beforeStatus = part.status();

        LocalDate afterEta =
                request.afterEta() == null
                        || request.afterEta().isBlank()
                        ? beforeEta
                        : LocalDate.parse(request.afterEta());

        String afterStatus =
                request.afterPartStatus() == null
                        || request.afterPartStatus().isBlank()
                        ? beforeStatus
                        : request.afterPartStatus().trim().toUpperCase();

        Map<String, ChangeImpactResponse.ChangeValue> changes =
                new LinkedHashMap<>();

        if (!same(
                beforeEta == null ? null : beforeEta.toString(),
                afterEta == null ? null : afterEta.toString()
        )) {
            changes.put(
                    "eta",
                    new ChangeImpactResponse.ChangeValue(
                            beforeEta == null ? null : beforeEta.toString(),
                            afterEta == null ? null : afterEta.toString()
                    )
            );
        }

        if (!same(beforeStatus, afterStatus)) {
            changes.put(
                    "partStatus",
                    new ChangeImpactResponse.ChangeValue(
                            beforeStatus,
                            afterStatus
                    )
            );
        }

        if (changes.isEmpty()) {
            throw new IllegalArgumentException(
                    "No actual before/after change was detected."
            );
        }

        String supplyBefore =
                calculateSupplyAlignment(
                        caseData.requestedServiceDate(),
                        beforeStatus,
                        beforeEta
                );

        String supplyAfter =
                calculateSupplyAlignment(
                        caseData.requestedServiceDate(),
                        afterStatus,
                        afterEta
                );

        /*
         * Reuse Feature 1 instead of inventing a previous commitment.
         * This means Change Impact uses the same evidence-backed
         * commitment interpretation already validated by Promise Integrity.
         */
        PromiseIntegrityResponse promise =
                promiseIntegrityService.analyze(caseId);

        LocalDate promisedDate =
                parseDate(promise.promisedDate());

        String promiseRiskBefore =
                calculatePromiseRisk(
                        promise.commitmentExists(),
                        beforeStatus,
                        beforeEta,
                        promisedDate
                );

        String promiseRiskAfter =
                calculatePromiseRisk(
                        promise.commitmentExists(),
                        afterStatus,
                        afterEta,
                        promisedDate
                );

        String appointment =
                caseData.appointment() == null
                        ? null
                        : caseData.appointment()
                                .scheduledAt()
                                .toString();

        String prompt = buildPrompt(
                changes,
                caseData.requestedServiceDate(),
                appointment,
                promise,
                supplyBefore,
                supplyAfter,
                promiseRiskBefore,
                promiseRiskAfter
        );

        StructuredResponseCreateParams<ChangeImpactReview> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .text(ChangeImpactReview.class)
                        .model(MODEL)
                        .build();

        ChangeImpactReview review =
                openAIClient.responses()
                        .create(params)
                        .output()
                        .stream()
                        .flatMap(item -> item.message().stream())
                        .flatMap(message -> message.content().stream())
                        .flatMap(content -> content.outputText().stream())
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "OpenAI returned no Change Impact output."
                                )
                        );

        return new ChangeImpactResponse(
                caseData.caseId(),
                "CHANGE_IMPACT_RADAR",
                MODEL,

                changes,

                caseData.requestedServiceDate() == null
                        ? null
                        : caseData.requestedServiceDate().toString(),

                appointment,

                promise.commitmentExists(),
                promise.evidence(),
                promise.promisedDate(),

                supplyBefore,
                supplyAfter,

                promiseRiskBefore,
                promiseRiskAfter,

                review.changeSummary,
                optional(review.affectedCustomerExpectation),
                review.affectedAppointment,
                optional(review.workflowImpact),

                review.evidenceFacts == null
                        ? List.of()
                        : review.evidenceFacts
        );
    }

    private String buildPrompt(
            Map<String, ChangeImpactResponse.ChangeValue> changes,
            LocalDate customerNeed,
            String appointment,
            PromiseIntegrityResponse promise,
            String supplyBefore,
            String supplyAfter,
            String promiseRiskBefore,
            String promiseRiskAfter
    ) {

        String changeText = changes.entrySet()
                .stream()
                .map(entry ->
                        "- " + entry.getKey()
                                + ": "
                                + entry.getValue().before()
                                + " -> "
                                + entry.getValue().after()
                )
                .reduce(
                        "",
                        (a, b) -> a + b + "\n"
                );

        return """
                ROLE:
                Recall service change impact reviewer.

                CONFIRMED CHANGE
                %s

                CURRENT CONTEXT
                - Customer requested service date: %s
                - Existing appointment: %s

                PREVIOUS CUSTOMER COMMITMENT
                - Commitment exists: %s
                - Evidence: %s
                - Normalized promised date: %s

                CODE-CALCULATED STATES
                - Supply alignment before: %s
                - Supply alignment after: %s
                - Promise risk before: %s
                - Promise risk after: %s

                TASK:
                Explain which existing customer expectation or workflow
                is affected by the confirmed change.

                IMPORTANT:
                Customer requested service date and explicit customer commitment
                are different concepts.

                If Commitment exists is false:
                - Do not invent a previous promise.
                - You may still explain impact on the customer's requested date
                  or existing appointment when supported by facts.

                CONSTRAINTS:
                1. Do not recalculate or change any CODE-CALCULATED STATES.
                2. Do not change database values.
                3. Do not create a Next Action.
                4. Do not invent a customer commitment.
                5. Do not invent facts outside the provided context.
                6. Evidence facts must come only from the provided context.
                """.formatted(
                        changeText,
                        value(customerNeed),
                        value(appointment),

                        promise.commitmentExists(),
                        value(promise.evidence()),
                        value(promise.promisedDate()),

                        supplyBefore,
                        supplyAfter,
                        promiseRiskBefore,
                        promiseRiskAfter
                );
    }

    private String calculateSupplyAlignment(
            LocalDate requestedDate,
            String partStatus,
            LocalDate eta
    ) {

        if ("ARRIVED".equalsIgnoreCase(partStatus)) {
            return "ALIGNED";
        }

        if (requestedDate == null || eta == null) {
            return "AT_RISK";
        }

        return eta.isAfter(requestedDate)
                ? "AT_RISK"
                : "WATCH";
    }

    private String calculatePromiseRisk(
            boolean commitmentExists,
            String partStatus,
            LocalDate eta,
            LocalDate promisedDate
    ) {

        if (!commitmentExists) {
            return "NO_COMMITMENT";
        }

        if ("ARRIVED".equalsIgnoreCase(partStatus)) {
            return "SAFE";
        }

        if (eta == null) {
            return "AT_RISK";
        }

        if (promisedDate == null) {
            return "NEEDS_REVIEW";
        }

        return eta.isAfter(promisedDate)
                ? "AT_RISK"
                : "WATCH";
    }

    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return LocalDate.parse(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean same(String a, String b) {
        if (a == null) return b == null;
        return a.equals(b);
    }

    private String optional(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }

    private String value(Object value) {
        return value == null ? "NONE" : value.toString();
    }
}
