package com.recallrelay.ai.contradiction;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public class ContradictionReview {

    @JsonPropertyDescription(
            "True only when at least one meaningful cross-source contradiction should be reviewed by a human."
    )
    public boolean hasPossibleConflict;

    @JsonPropertyDescription(
            "Meaningful contradiction candidates. Empty when no contradiction exists."
    )
    public List<ConflictItem> conflicts;

    public static class ConflictItem {

        @JsonPropertyDescription(
                "First conflicting source, for example Current Part Record."
        )
        public String sourceA;

        @JsonPropertyDescription(
                "Second conflicting source, for example Timeline Activity 51010."
        )
        public String sourceB;

        @JsonPropertyDescription(
                "Why these two sources cannot both describe the same current state."
        )
        public String reason;

        @JsonPropertyDescription(
                "Exact supporting facts from the provided inputs."
        )
        public List<String> evidence;
    }
}
