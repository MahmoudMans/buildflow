package com.buildflow.search.dto;

import java.util.List;
import java.util.UUID;

public final class SearchDtos {

    private SearchDtos() {
    }

    /**
     * One search result.
     *
     * kind is one of PROJECT, TASK, CLIENT, SUPPLIER, INVOICE or
     * PURCHASE_ORDER. projectId is the owning project (for a PROJECT hit it
     * equals id) and is null for clients and suppliers.
     */
    public record SearchHit(
        String kind,
        UUID id,
        UUID projectId,
        String title,
        String subtitle,
        String status
    ) {
    }

    public record SearchResponse(
        String query,
        List<SearchHit> hits
    ) {
    }
}
