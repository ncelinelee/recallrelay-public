package com.recallrelay.ai.nextquestion;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Optional;

public class NextBestQuestionReview {

    @JsonPropertyDescription(
            "Question the employee should use to verify the first code-detected missing field."
    )
    public Optional<String> verifyFirst = Optional.empty();

    @JsonPropertyDescription(
            "Why that code-detected information is needed for the current recall case."
    )
    public Optional<String> reason = Optional.empty();

    @JsonPropertyDescription(
            "Facts from CURRENT FACTS that support the explanation."
    )
    public List<String> evidenceFacts;
}
