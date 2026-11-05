package com.recallrelay.ai.overpromise;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.recallrelay.casework.RecallCaseQueryService;
import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class OverpromiseService {

    private static final String MODEL = "gpt-5.6";

    private final OpenAIClient openAIClient;
    private final RecallCaseQueryService recallCaseQueryService;

    public OverpromiseService(
            OpenAIClient openAIClient,
            RecallCaseQueryService recallCaseQueryService
    ) {
        this.openAIClient = openAIClient;
        this.recallCaseQueryService = recallCaseQueryService;
    }

    public OverpromiseResponse analyze(
            Long caseId,
            OverpromiseRequest request
    ) {

        if (request == null
                || request.draft() == null
                || request.draft().isBlank()) {
            throw new IllegalArgumentException(
                    "Customer draft is required."
            );
        }

        RecallCaseDetailResponse caseData =
                recallCaseQueryService.getCaseDetail(caseId);

        RecallCaseDetailResponse.PartResponse part =
                caseData.parts() == null || caseData.parts().isEmpty()
                        ? null
                        : caseData.parts().get(0);

        RecallCaseDetailResponse.AppointmentResponse appointment =
                caseData.appointment();

        boolean arrived =
                part != null
                        && "ARRIVED".equalsIgnoreCase(part.status())
                        && part.arrivedAt() != null;

        String alignment =
                calculateSupplyAlignment(
                        caseData.requestedServiceDate(),
                        part
                );

        String prompt = buildPrompt(
                request.draft(),
                part,
                arrived,
                appointment,
                alignment
        );

        StructuredResponseCreateParams<OverpromiseReview> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .text(OverpromiseReview.class)
                        .model(MODEL)
                        .build();

        OverpromiseReview review =
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
                                        "OpenAI returned no Overpromise Guard output."
                                )
                        );

        return new OverpromiseResponse(
                caseData.caseId(),
                "OVERPROMISE_GUARD",
                MODEL,
                request.draft(),

                part == null ? null : part.status(),
                part == null || part.eta() == null
                        ? null
                        : part.eta().toString(),

                arrived,

                appointment == null
                        ? null
                        : appointment.scheduledAt().toString(),

                alignment,

                review.risk,
                optional(review.unsupportedPhrase),
                optional(review.reason),
                optional(review.saferWording),

                review.evidenceFacts == null
                        ? List.of()
                        : review.evidenceFacts
        );
    }

    private String buildPrompt(
            String draft,
            RecallCaseDetailResponse.PartResponse part,
            boolean arrived,
            RecallCaseDetailResponse.AppointmentResponse appointment,
            String alignment
    ) {

        return """
                ROLE:
                Communication risk reviewer for a recall service team.

                CURRENT FACTS
                - Part status: %s
                - ETA: %s
                - Physical arrival confirmed: %s
                - Appointment: %s
                - Supply alignment: %s

                DRAFT TO CUSTOMER
                %s

                TASK:
                Determine whether the customer-facing wording is more certain
                than the current facts support.

                GUIDANCE:
                - ETA means expected arrival, not guaranteed arrival.
                - "will arrive", "guaranteed", or equivalent certainty requires
                  stronger evidence than ETA alone.
                - "expected to arrive" may be supported by a valid ETA.
                - "has arrived" requires confirmed physical arrival.
                - A scheduled appointment does not guarantee part availability.

                CONSTRAINTS:
                1. Do not change operational status.
                2. Do not change Supply Alignment.
                3. Do not create or change Next Action.
                4. Do not modify database values.
                5. Do not invent facts.
                6. Safer wording must remain inside CURRENT FACTS.
                7. Evidence facts must come only from CURRENT FACTS.
                """.formatted(
                        part == null ? "UNKNOWN" : part.status(),
                        part == null || part.eta() == null
                                ? "UNKNOWN"
                                : part.eta(),
                        arrived ? "YES" : "NO",
                        appointment == null
                                ? "NONE"
                                : appointment.scheduledAt(),
                        alignment,
                        draft
                );
    }

    private String calculateSupplyAlignment(
            LocalDate requestedDate,
            RecallCaseDetailResponse.PartResponse part
    ) {

        if (part == null) return "AT_RISK";

        if ("ARRIVED".equalsIgnoreCase(part.status())) {
            return "ALIGNED";
        }

        if (requestedDate == null || part.eta() == null) {
            return "AT_RISK";
        }

        if (part.eta().isAfter(requestedDate)) {
            return "AT_RISK";
        }

        return "WATCH";
    }

    private String optional(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }
}
