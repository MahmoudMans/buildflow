package com.buildflow.search.controller;

import com.buildflow.search.dto.SearchDtos;
import com.buildflow.search.service.SearchService;
import com.buildflow.security.BuildFlowPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public SearchDtos.SearchResponse search(
        @RequestHeader("X-Organization-Id")
        UUID organizationId,
        @RequestParam(name = "q", required = false, defaultValue = "")
        String query,
        @AuthenticationPrincipal
        BuildFlowPrincipal principal
    ) {
        return searchService.search(
            organizationId,
            principal.userId(),
            query
        );
    }
}
