package com.recallrelay.ai.contradiction;

import java.util.List;

public record ContradictionResponse(
        Long caseId,
        String feature,
        String model,

        String partStatus,
        String partStateTimestamp,

        int activitiesReviewed,
        List<TemporalObservation> temporalObservations,

        boolean llmUsed,
        boolean hasPossibleConflict,
        List<Conflict> conflicts
) {

    public record TemporalObservation(
            Long activityId,
            String summary,
            String activityTimestamp,
            String temporalRelation
    ) {}

    public record Conflict(
            String sourceA,
            String sourceB,
            String reason,
            List<String> evidence
    ) {}
}
