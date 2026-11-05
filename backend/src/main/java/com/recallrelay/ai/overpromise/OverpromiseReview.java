package com.recallrelay.ai.overpromise;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;
import java.util.Optional;

public class OverpromiseReview {

    @JsonPropertyDescription(
            "Communication risk. Must be NONE, LOW, or HIGH."
    )
    public String risk;

    @JsonPropertyDescription(
            "Exact phrase from the draft that is more certain than current facts support."
    )
    public Optional<String> unsupportedPhrase = Optional.empty();

    @JsonPropertyDescription(
            "Why the wording is or is not supported by current facts."
    )
    public Optional<String> reason = Optional.empty();

    @JsonPropertyDescription(
            "Safer wording using only information supported by current facts."
    )
    public Optional<String> saferWording = Optional.empty();

    @JsonPropertyDescription(
            "Current facts used as evidence."
    )
    public List<String> evidenceFacts;
}
