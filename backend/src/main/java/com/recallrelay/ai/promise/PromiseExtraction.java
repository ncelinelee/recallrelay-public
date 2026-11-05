package com.recallrelay.ai.promise;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.Optional;

public class PromiseExtraction {

    @JsonPropertyDescription(
            "True only when the interaction explicitly contains a customer-facing commitment."
    )
    public boolean exists;

    @JsonPropertyDescription(
            "Type of customer-facing commitment, for example Part arrival or Service timing."
    )
    public Optional<String> commitmentType = Optional.empty();

    @JsonPropertyDescription(
            "The date or condition stated in the commitment, for example Before Sep 3."
    )
    public Optional<String> dateOrCondition = Optional.empty();

    @JsonPropertyDescription(
            "Exact supporting words copied from the customer interaction. Do not paraphrase."
    )
    public Optional<String> evidence = Optional.empty();
}
