package com.recallrelay.ai.contradiction;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.recallrelay.casework.RecallCaseQueryService;
import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContradictionService {

    private static final String MODEL = "gpt-5.6";

    private final OpenAIClient openAIClient;
    private final RecallCaseQueryService recallCaseQueryService;

    public ContradictionService(
            OpenAIClient openAIClient,
            RecallCaseQueryService recallCaseQueryService
    ) {
        this.openAIClient = openAIClient;
        this.recallCaseQueryService = recallCaseQueryService;
    }

    public ContradictionResponse analyze(Long caseId) {

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

        LocalDateTime partStateTimestamp =
                resolvePartStateTimestamp(part);

        List<ContradictionResponse.TemporalObservation> observations =
                caseData.activities() == null
                        ? List.of()
                        : caseData.activities()
                            .stream()
                            .filter(activity ->
                                    activity.summary() != null
                                    && !activity.summary().isBlank()
                            )
                            .map(activity ->
                                    new ContradictionResponse.TemporalObservation(
                                            activity.activityId(),
                                            activity.summary(),
                                            activity.createdAt() == null
                                                    ? null
                                                    : activity.createdAt().toString(),
                                            temporalRelation(
                                                    activity.createdAt(),
                                                    partStateTimestamp
                                            )
                                    )
                            )
                            .toList();

        /*
         * Timeline이 없으면 의미 비교 자체가 불가능하므로
         * OpenAI를 호출하지 않는다.
         */
        if (observations.isEmpty()) {
            return new ContradictionResponse(
                    caseData.caseId(),
                    "CROSS_SOURCE_CONTRADICTION",
                    MODEL,

                    part.status(),
                    partStateTimestamp == null
                            ? null
                            : partStateTimestamp.toString(),

                    0,
                    List.of(),

                    false,
                    false,
                    List.of()
            );
        }

        String prompt = buildPrompt(
                part.status(),
                partStateTimestamp,
                observations
        );

        StructuredResponseCreateParams<ContradictionReview> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .text(ContradictionReview.class)
                        .model(MODEL)
                        .build();

        ContradictionReview review =
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
                                        "OpenAI returned no Contradiction Detector output."
                                )
                        );

        List<ContradictionResponse.Conflict> conflicts =
                review.conflicts == null
                        ? List.of()
                        : review.conflicts
                            .stream()
                            .map(conflict ->
                                    new ContradictionResponse.Conflict(
                                            conflict.sourceA,
                                            conflict.sourceB,
                                            conflict.reason,
                                            conflict.evidence == null
                                                    ? List.of()
                                                    : conflict.evidence
                                    )
                            )
                            .toList();

        return new ContradictionResponse(
                caseData.caseId(),
                "CROSS_SOURCE_CONTRADICTION",
                MODEL,

                part.status(),
                partStateTimestamp == null
                        ? null
                        : partStateTimestamp.toString(),

                observations.size(),
                observations,

                true,
                review.hasPossibleConflict,
                conflicts
        );
    }

    private LocalDateTime resolvePartStateTimestamp(
            RecallCaseDetailResponse.PartResponse part
    ) {

        /*
         * ARRIVED는 물리 도착 시각이 존재하면 그것을 우선한다.
         * 그 외 상태는 현재 Part Record updatedAt을 기준으로 한다.
         */
        if (
                "ARRIVED".equalsIgnoreCase(part.status())
                && part.arrivedAt() != null
        ) {
            return part.arrivedAt();
        }

        return part.updatedAt();
    }

    private String temporalRelation(
            LocalDateTime activityTimestamp,
            LocalDateTime partStateTimestamp
    ) {

        if (
                activityTimestamp == null
                || partStateTimestamp == null
        ) {
            return "UNKNOWN";
        }

        if (activityTimestamp.isBefore(partStateTimestamp)) {
            return "INTERACTION_BEFORE_PART_RECORD";
        }

        if (activityTimestamp.isAfter(partStateTimestamp)) {
            return "INTERACTION_AFTER_PART_RECORD";
        }

        return "SAME_TIME";
    }

    private String buildPrompt(
            String partStatus,
            LocalDateTime partStateTimestamp,
            List<ContradictionResponse.TemporalObservation> observations
    ) {

        String timeline = observations
                .stream()
                .map(observation ->
                        """
                        - Activity ID: %s
                          Text: %s
                          Timestamp: %s
                          Temporal relation: %s
                        """.formatted(
                                observation.activityId(),
                                observation.summary(),
                                observation.activityTimestamp(),
                                observation.temporalRelation()
                        )
                )
                .collect(Collectors.joining("\n"));

        return """
                ROLE:
                Recall service cross-source consistency reviewer.

                CURRENT PART RECORD
                - Current part status: %s
                - Current part-state timestamp: %s

                TIMELINE ACTIVITIES WITH CODE-CALCULATED TEMPORAL RELATION
                %s

                TASK:
                Identify only meaningful semantic contradictions
                between the CURRENT PART RECORD and Timeline activities.

                TEMPORAL RULE:
                - INTERACTION_BEFORE_PART_RECORD represents historical context.
                  Do not flag it solely because it describes an older state.
                - INTERACTION_AFTER_PART_RECORD can be a contradiction candidate
                  only when its semantic meaning conflicts with the current Part state.
                - SAME_TIME requires cautious review.
                - An unrelated Timeline statement is not a contradiction.

                EXAMPLES OF MEANING:
                - Current Part = ARRIVED
                  Later Timeline = "The part is still in transit."
                  -> possible contradiction.

                - Historical Timeline = "The part is still in transit."
                  Later Current Part = ARRIVED
                  -> normal state evolution, not automatically a contradiction.

                CONSTRAINTS:
                1. Do not modify database values.
                2. Do not decide which source is correct.
                3. Do not create or change a Next Action.
                4. Do not invent facts.
                5. Do not treat historical state evolution as an error.
                6. Do not flag unrelated Timeline text.
                7. Evidence must come only from the provided Part Record
                   and Timeline activities.
                8. Output only human-review contradiction candidates.
                """.formatted(
                        partStatus,
                        partStateTimestamp,
                        timeline
                );
    }
}
