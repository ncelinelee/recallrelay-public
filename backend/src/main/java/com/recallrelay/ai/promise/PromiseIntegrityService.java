package com.recallrelay.ai.promise;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.recallrelay.casework.RecallCaseQueryService;
import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class PromiseIntegrityService {

    private static final String MODEL = "gpt-5.6";

    private static final Pattern MONTH_DAY_PATTERN = Pattern.compile(
            "(?i)\\b" +
            "(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|" +
            "Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:t(?:ember)?)?|" +
            "Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)" +
            "\\s+(\\d{1,2})(?:,?\\s+(\\d{4}))?\\b"
    );

    private final OpenAIClient openAIClient;
    private final RecallCaseQueryService recallCaseQueryService;

    public PromiseIntegrityService(
            OpenAIClient openAIClient,
            RecallCaseQueryService recallCaseQueryService
    ) {
        this.openAIClient = openAIClient;
        this.recallCaseQueryService = recallCaseQueryService;
    }

    public PromiseIntegrityResponse analyze(Long caseId) {

        RecallCaseDetailResponse caseData =
                recallCaseQueryService.getCaseDetail(caseId);

        RecallCaseDetailResponse.PartResponse part =
                caseData.parts() == null || caseData.parts().isEmpty()
                        ? null
                        : caseData.parts().get(0);

        RecallCaseDetailResponse.AppointmentResponse appointment =
                caseData.appointment();

        String customerInteractions = caseData.activities() == null
                ? ""
                : caseData.activities().stream()
                    .filter(activity ->
                            "CONTACT".equalsIgnoreCase(activity.activityType())
                    )
                    .map(activity ->
                            "[" + value(activity.createdAt()) + "] "
                                    + activity.summary()
                    )
                    .collect(Collectors.joining("\n"));

        if (customerInteractions.isBlank()) {
            customerInteractions = "No customer-facing interaction recorded.";
        }

        String prompt = buildPrompt(
                caseData,
                part,
                appointment,
                customerInteractions
        );

        StructuredResponseCreateParams<PromiseExtraction> params =
                ResponseCreateParams.builder()
                        .input(prompt)
                        .text(PromiseExtraction.class)
                        .model(MODEL)
                        .build();

        PromiseExtraction extraction =
                openAIClient.responses()
                        .create(params)
                        .output()
                        .stream()
                        .flatMap(item -> item.message().stream())
                        .flatMap(message -> message.content().stream())
                        .flatMap(content -> content.outputText().stream())
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "OpenAI returned no structured Promise Integrity output."
                                )
                        );

        String commitmentType =
                optionalValue(extraction.commitmentType);

        String dateOrCondition =
                optionalValue(extraction.dateOrCondition);

        String evidence =
                optionalValue(extraction.evidence);

        LocalDate promisedDate =
                extraction.exists
                        ? parsePromisedDate(
                                dateOrCondition,
                                caseData.requestedServiceDate()
                        )
                        : null;

        RuleResult rule =
                evaluateRule(
                        extraction.exists,
                        part,
                        promisedDate
                );

        return new PromiseIntegrityResponse(
                caseData.caseId(),
                "PROMISE_INTEGRITY",
                MODEL,

                extraction.exists,
                commitmentType,
                dateOrCondition,
                evidence,

                promisedDate == null
                        ? null
                        : promisedDate.toString(),

                part == null
                        ? null
                        : part.status(),

                part == null || part.eta() == null
                        ? null
                        : part.eta().toString(),

                rule.status(),
                rule.reason()
        );
    }

    private String buildPrompt(
            RecallCaseDetailResponse caseData,
            RecallCaseDetailResponse.PartResponse part,
            RecallCaseDetailResponse.AppointmentResponse appointment,
            String customerInteractions
    ) {

        return """
                ROLE:
                You are a Service Manager reviewing a Recall service case.

                TASK:
                Extract only customer-facing commitments from the recorded interaction.

                CURRENT FACTS
                - Customer requested service date: %s
                - Part status: %s
                - Part ETA: %s
                - Appointment: %s

                CUSTOMER INTERACTION TIMELINE
                %s

                CONSTRAINTS
                1. Do not invent a commitment.
                2. A database fact is not automatically a customer commitment.
                3. The existence of an appointment is not itself evidence of a promise.
                4. Only classify something as a commitment when the interaction explicitly
                   shows that timing, arrival, service, or another future condition was
                   communicated to the customer.
                5. If evidence is insufficient, exists must be false and optional fields
                   should be empty.
                6. Evidence must be copied exactly from the interaction.
                7. Do not decide Supply Alignment.
                8. Do not create or modify a Next Action.
                9. Do not modify any database state.
                """.formatted(
                        value(caseData.requestedServiceDate()),
                        part == null ? "UNKNOWN" : value(part.status()),
                        part == null ? "UNKNOWN" : value(part.eta()),
                        appointment == null
                                ? "NONE"
                                : value(appointment.scheduledAt()),
                        customerInteractions
                );
    }

    private RuleResult evaluateRule(
            boolean commitmentExists,
            RecallCaseDetailResponse.PartResponse part,
            LocalDate promisedDate
    ) {

        if (!commitmentExists) {
            return new RuleResult(
                    "NO_COMMITMENT",
                    "No explicit customer-facing commitment was found in the recorded interaction."
            );
        }

        if (part == null) {
            return new RuleResult(
                    "AT_RISK",
                    "A customer commitment exists, but no recall part record is available."
            );
        }

        if ("ARRIVED".equalsIgnoreCase(part.status())) {
            return new RuleResult(
                    "SAFE",
                    "The required recall part has physically arrived."
            );
        }

        if (part.eta() == null) {
            return new RuleResult(
                    "AT_RISK",
                    "A customer commitment exists, but the current part ETA is unavailable."
            );
        }

        if (promisedDate == null) {
            return new RuleResult(
                    "NEEDS_REVIEW",
                    "A commitment was found, but its timing could not be normalized into a date for deterministic comparison."
            );
        }

        if (part.eta().isAfter(promisedDate)) {
            return new RuleResult(
                    "AT_RISK",
                    "Current part ETA is later than the customer-facing commitment date."
            );
        }

        return new RuleResult(
                "WATCH",
                "Current ETA is on or before the commitment date, but the part has not physically arrived."
        );
    }

    private LocalDate parsePromisedDate(
            String dateOrCondition,
            LocalDate referenceDate
    ) {

        if (dateOrCondition == null || dateOrCondition.isBlank()) {
            return null;
        }

        String text = dateOrCondition.trim();

        try {
            return LocalDate.parse(text);
        } catch (Exception ignored) {
        }

        Matcher matcher = MONTH_DAY_PATTERN.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        Month month = parseMonth(matcher.group(1));
        int day = Integer.parseInt(matcher.group(2));

        int year;

        if (matcher.group(3) != null) {
            year = Integer.parseInt(matcher.group(3));
        } else if (referenceDate != null) {
            year = referenceDate.getYear();
        } else {
            year = LocalDate.now().getYear();
        }

        try {
            return LocalDate.of(year, month, day);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Month parseMonth(String value) {

        return switch (value.substring(0, 3).toLowerCase()) {
            case "jan" -> Month.JANUARY;
            case "feb" -> Month.FEBRUARY;
            case "mar" -> Month.MARCH;
            case "apr" -> Month.APRIL;
            case "may" -> Month.MAY;
            case "jun" -> Month.JUNE;
            case "jul" -> Month.JULY;
            case "aug" -> Month.AUGUST;
            case "sep" -> Month.SEPTEMBER;
            case "oct" -> Month.OCTOBER;
            case "nov" -> Month.NOVEMBER;
            case "dec" -> Month.DECEMBER;
            default -> throw new IllegalArgumentException(
                    "Unsupported month: " + value
            );
        };
    }

    private String optionalValue(Optional<String> value) {
        return value == null ? null : value.orElse(null);
    }

    private String value(Object value) {
        return value == null ? "UNKNOWN" : value.toString();
    }

    private record RuleResult(
            String status,
            String reason
    ) {}
}
