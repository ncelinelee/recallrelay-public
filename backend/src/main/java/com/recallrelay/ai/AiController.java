package com.recallrelay.ai;

import com.recallrelay.ai.changeimpact.ChangeImpactRequest;
import com.recallrelay.ai.changeimpact.ChangeImpactResponse;
import com.recallrelay.ai.changeimpact.ChangeImpactService;
import com.recallrelay.ai.contradiction.ContradictionResponse;
import com.recallrelay.ai.contradiction.ContradictionService;
import com.recallrelay.ai.nextquestion.NextBestQuestionResponse;
import com.recallrelay.ai.nextquestion.NextBestQuestionService;
import com.recallrelay.ai.overpromise.OverpromiseRequest;
import com.recallrelay.ai.overpromise.OverpromiseResponse;
import com.recallrelay.ai.overpromise.OverpromiseService;
import com.recallrelay.ai.promise.PromiseIntegrityResponse;
import com.recallrelay.ai.promise.PromiseIntegrityService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/cases")
public class AiController {

    private final PromiseIntegrityService promiseIntegrityService;
    private final OverpromiseService overpromiseService;
    private final ChangeImpactService changeImpactService;
    private final NextBestQuestionService nextBestQuestionService;
    private final ContradictionService contradictionService;

    public AiController(
            PromiseIntegrityService promiseIntegrityService,
            OverpromiseService overpromiseService,
            ChangeImpactService changeImpactService,
            NextBestQuestionService nextBestQuestionService,
            ContradictionService contradictionService
    ) {
        this.promiseIntegrityService = promiseIntegrityService;
        this.overpromiseService = overpromiseService;
        this.changeImpactService = changeImpactService;
        this.nextBestQuestionService = nextBestQuestionService;
        this.contradictionService = contradictionService;
    }

    @PostMapping("/{caseId}/promise-integrity")
    public PromiseIntegrityResponse analyzePromiseIntegrity(
            @PathVariable Long caseId
    ) {
        return promiseIntegrityService.analyze(caseId);
    }

    @PostMapping("/{caseId}/overpromise")
    public OverpromiseResponse analyzeOverpromise(
            @PathVariable Long caseId,
            @RequestBody OverpromiseRequest request
    ) {
        return overpromiseService.analyze(caseId, request);
    }

    @PostMapping("/{caseId}/change-impact")
    public ChangeImpactResponse analyzeChangeImpact(
            @PathVariable Long caseId,
            @RequestBody ChangeImpactRequest request
    ) {
        return changeImpactService.analyze(caseId, request);
    }

    @PostMapping("/{caseId}/next-best-question")
    public NextBestQuestionResponse analyzeNextBestQuestion(
            @PathVariable Long caseId
    ) {
        return nextBestQuestionService.analyze(caseId);
    }

    @PostMapping("/{caseId}/contradictions")
    public ContradictionResponse analyzeContradictions(
            @PathVariable Long caseId
    ) {
        return contradictionService.analyze(caseId);
    }
}
