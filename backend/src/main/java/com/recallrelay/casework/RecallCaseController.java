package com.recallrelay.casework;

import com.recallrelay.casework.dto.RecallCaseDetailResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cases")
public class RecallCaseController {

    private final RecallCaseQueryService recallCaseQueryService;

    public RecallCaseController(RecallCaseQueryService recallCaseQueryService) {
        this.recallCaseQueryService = recallCaseQueryService;
    }

    @GetMapping("/{caseId}")
    public RecallCaseDetailResponse getCase(
            @PathVariable Long caseId
    ) {
        return recallCaseQueryService.getCaseDetail(caseId);
    }
}
