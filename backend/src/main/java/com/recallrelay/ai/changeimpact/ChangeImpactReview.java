package com.recallrelay.ai.changeimpact;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Optional;

public class ChangeImpactReview {

    @JsonPropertyDescription(
            "Concise explanation of the confirmed before/after change."
    )
    public String changeSummary;

    @JsonPropertyDescription(
            "Existing customer expectation affected by the change. Empty if none."
    )
    public Optional<String> affectedCustomerExpectation = Optional.empty();

    @JsonPropertyDescription(
            "True only when the existing appointment is meaningfully affected by the confirmed change."
    )
    public boolean affectedAppointment;

    @JsonPropertyDescription(
            "Operational workflow impact caused by the confirmed change. Do not create a Next Action."
    )
    public Optional<String> workflowImpact = Optional.empty();

    @JsonPropertyDescription(
            "Facts from the provided context that support the impact explanation."
    )
    public List<String> evidenceFacts;
}
