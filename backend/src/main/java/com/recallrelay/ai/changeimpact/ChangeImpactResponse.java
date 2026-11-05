package com.recallrelay.ai.changeimpact;

import java.util.List;
import java.util.Map;

public record ChangeImpactResponse(
        Long caseId,
        String feature,
        String model,

        Map<String, ChangeValue> changes,

        String requestedServiceDate,
        String appointment,

        boolean previousCommitmentExists,
        String previousCommitmentEvidence,
        String promisedDate,

        String supplyAlignmentBefore,
        String supplyAlignmentAfter,

        String promiseRiskBefore,
        String promiseRiskAfter,

        String changeSummary,
        String affectedCustomerExpectation,
        boolean affectedAppointment,
        String workflowImpact,
        List<String> evidenceFacts
) {
    public record ChangeValue(
            String before,
            String after
    ) {}
}
