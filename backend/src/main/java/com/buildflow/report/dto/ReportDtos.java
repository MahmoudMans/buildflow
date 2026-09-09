package com.buildflow.report.dto;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record ReportFile(
        byte[] content,
        String filename,
        String contentType
    ) {
    }
}
