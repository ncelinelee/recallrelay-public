# Prompt Experiment Lessons Learned

## Experiment
Compared three prompt versions across three predefined ground-truth cases.

## v1 - Zero-shot
- Free-text responses were not reliably machine-readable.
- No explicit evidence field was returned.
- The same interaction in Cases B and C produced inconsistent commitment judgments.

## v2 - Few-shot
- JSON output became consistent.
- However, the negative ETA example caused false negatives in Cases B and C.
- A system ETA fact and an ETA-related statement communicated to a customer need to be distinguished.

## v3 - Context and Guardrails
- Added current case facts, task boundaries, explicit constraints, JSON output, and evidence requirements.
- All three initial ground-truth cases produced the expected commitment and date/condition.
- Evidence was returned from the original interaction.
- No operational status or Next Action was delegated to the LLM.

## Key Design Decision
Use the LLM to interpret natural-language customer commitments.
Use deterministic rules for supply-risk decisions such as comparing ETA against the promised date.

## Limitation
This was an initial experiment using three controlled ground-truth cases. Results should not be generalized as production accuracy.
