package com.recallrelay.ai.promise;

public record PromiseIntegrityResponse(
        Long caseId,
        String feature,
        String model,

        boolean commitmentExists,
        String commitmentType,
        String dateOrCondition,
        String evidence,

        String promisedDate,

        String partStatus,
        String eta,

        String ruleStatus,
        String ruleReason
) {}
