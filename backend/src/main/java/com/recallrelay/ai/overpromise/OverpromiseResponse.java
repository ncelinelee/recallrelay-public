package com.recallrelay.ai.overpromise;

import java.util.List;

public record OverpromiseResponse(
        Long caseId,
        String feature,
        String model,
        String draft,

        String partStatus,
        String eta,
        boolean physicalArrivalConfirmed,
        String appointment,
        String supplyAlignment,

        String risk,
        String unsupportedPhrase,
        String reason,
        String saferWording,
        List<String> evidenceFacts
) {}
