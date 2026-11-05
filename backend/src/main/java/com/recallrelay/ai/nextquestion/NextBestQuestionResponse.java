package com.recallrelay.ai.nextquestion;

import java.util.List;

public record NextBestQuestionResponse(
        Long caseId,
        String feature,
        String model,

        String partStatus,
        String eta,
        String requestedServiceDate,

        boolean informationMissing,
        List<String> missingFields,

        boolean llmUsed,
        String verifyFirst,
        String reason,
        List<String> evidenceFacts
) {}
