package com.recallrelay.ai.nextquestion;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.recallrelay.casework.RecallCaseQueryService;
import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class NextBestQuestionService {

    private static final String MODEL = "gpt-5.6";

    private final OpenAIClient openAIClient;
    private final RecallCaseQueryService recallCaseQueryService;

    public NextBestQuestionService(
            OpenAIClient openAIClient,
            RecallCaseQueryService recallCaseQueryService
    ) {
        this.openAIClient = openAIClient;
        this.recallCaseQueryService = recallCaseQueryService;
    }

    public NextBestQuestionResponse analyze(Long caseId) {

        RecallCaseDetailResponse caseData =
                recallCaseQueryService.getCaseDetail(caseId);

        RecallCaseDetailResponse.PartResponse part =
                caseData.parts() == null || caseData.parts().isEmpty()
                        ? null
                        : caseData.parts().get(0);

        String partStatus =
                part == null ? null : part.status();

        String eta =
                part == null || part.eta() == null
                        ? null
                        : part.eta().toString();

        String requestedDate =
                caseData.requestedServiceDate() == null
                        ? null
                        : caseData.requestedServiceDate().toString();

        /*
         * Deterministic Rule:
         * Missing Field 자체는 LLM이 판단하지 않는다.
         */
        List<String> missingFields =
                findMissingInformation(
                        partStatus,
                        eta,
                        requestedDate
                );

        /*
         * 필요한 정보가 없다면 OpenAI를 호출하지 않는다.
         * False Positive와 불필요한 API 비용을 동시에 방지한다.
         */
        if (missingFields.isEmpty()) {
            return new NextBestQuestionResponse(
                    caseData.caseId(),
                    "NEXT_BEST_QUESTION",
                    MODEL,

                    partStatus,
                    eta,
                    requestedDate,

                    false,
                    List.of(),

                    false,
                    null,
                    null,
                    List.of()
            );
        }

        String prompt = buildPrompt(
                partStatus,
                eta,
                requestedDate,
                missingFields
        );

        StructuredResponseCreateParams<NextBestQuestionReview> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .text(NextBestQuestionReview.class)
                        .model(MODEL)
                        .build();

        NextBestQuestionReview review =
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
                                        "OpenAI returned no Next Best Question output."
                                )
                        );

        return new NextBestQuestionResponse(
                caseData.caseId(),
                "NEXT_BEST_QUESTION",
                MODEL,

                partStatus,
                eta,
                requestedDate,

                true,
                List.copyOf(missingFields),

                true,
                optional(review.verifyFirst),
                optional(review.reason),

                review.evidenceFacts == null
                        ? List.of()
                        : review.evidenceFacts
        );
    }

    private List<String> findMissingInformation(
            String partStatus,
            String eta,
            String requestedDate
    ) {

        List<String> missing = new ArrayList<>();

        if (
                (
                    "ORDERED".equalsIgnoreCase(partStatus)
                    || "IN_TRANSIT".equalsIgnoreCase(partStatus)
                )
                && (eta == null || eta.isBlank())
        ) {
            missing.add("LATEST_PART_ETA");
        }

        if (requestedDate == null || requestedDate.isBlank()) {
            missing.add("CUSTOMER_REQUESTED_DATE");
        }

        return missing;
    }

    private String buildPrompt(
            String partStatus,
            String eta,
            String requestedDate,
            List<String> missingFields
    ) {

        return """
                ROLE:
                Recall service information-gap reviewer.

                CURRENT FACTS
                - Part status: %s
                - ETA: %s
                - Customer requested service date: %s

                CODE-DETECTED MISSING FIELDS
                %s

                FIELD DEFINITIONS
                - LATEST_PART_ETA:
                  latest expected arrival date for the recall part

                - CUSTOMER_REQUESTED_DATE:
                  date requested by the customer for recall service

                TASK:
                Explain which CODE-DETECTED missing information
                should be verified first and why it is needed
                for the current recall case.

                CONSTRAINTS:
                1. Do not invent missing values.
                2. Do not create an ETA.
                3. Do not create a customer requested service date.
                4. Use only the CODE-DETECTED MISSING FIELDS above.
                5. Do not suggest a system, supplier, department,
                   or information source unless it exists in CURRENT FACTS.
                6. Preserve the exact business meaning of FIELD DEFINITIONS.
                7. Do not change database values.
                8. Do not change operational status.
                9. Do not create or change a Next Action.
                10. Evidence must come only from CURRENT FACTS.
                """.formatted(
                        value(partStatus),
                        value(eta),
                        value(requestedDate),
                        missingFields
                );
    }

    private String optional(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }

    private String value(Object value) {
        return value == null ? "MISSING" : value.toString();
    }
}
