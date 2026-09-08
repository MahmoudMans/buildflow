package com.buildflow.siteops.controller;

import com.buildflow.security.BuildFlowPrincipal;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.DailyReportPhoto;
import com.buildflow.siteops.service.DailyReportPhotoService;
import com.buildflow.siteops.service.DailySiteReportService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DailySiteReportController {

    private final DailySiteReportService
        reportService;

    private final DailyReportPhotoService
        photoService;

    public DailySiteReportController(
        DailySiteReportService
            reportService,
        DailyReportPhotoService
            photoService
    ) {
        this.reportService =
            reportService;

        this.photoService =
            photoService;
    }

    @GetMapping(
        "/api/v1/site-reports/{reportId}"
    )
    public SiteOperationDtos.DailyReportView
        get(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID reportId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return reportService.get(
            organizationId,
            principal.userId(),
            reportId
        );
    }

    @PostMapping(
        "/api/v1/site-reports/{reportId}/update"
    )
    public SiteOperationDtos.DailyReportView
        update(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID reportId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            SiteOperationDtos
                .UpdateDailyReportRequest request
        ) {

        return reportService.update(
            organizationId,
            principal.userId(),
            reportId,
            request
        );
    }

    @GetMapping(
        "/api/v1/site-reports/{reportId}/photos"
    )
    public List<SiteOperationDtos.PhotoView>
        photos(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID reportId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        return photoService.list(
            organizationId,
            principal.userId(),
            reportId
        );
    }

    @PostMapping(
        "/api/v1/site-reports/{reportId}/photos"
    )
    public SiteOperationDtos.PhotoView
        uploadPhoto(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID reportId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal,
            @Valid
            @RequestBody
            SiteOperationDtos
                .UploadPhotoRequest request
        ) {

        return photoService.upload(
            organizationId,
            principal.userId(),
            reportId,
            request
        );
    }

    @GetMapping(
        "/api/v1/site-report-photos/{photoId}/content"
    )
    public ResponseEntity<byte[]>
        photoContent(
            @RequestHeader(
                "X-Organization-Id"
            )
            UUID organizationId,
            @PathVariable
            UUID photoId,
            @AuthenticationPrincipal
            BuildFlowPrincipal principal
        ) {

        DailyReportPhoto photo =
            photoService.content(
                organizationId,
                principal.userId(),
                photoId
            );

        HttpHeaders headers =
            new HttpHeaders();

        headers.setContentType(
            MediaType.parseMediaType(
                photo.getMimeType()
            )
        );

        headers.setContentDisposition(
            ContentDisposition
                .inline()
                .filename(
                    photo.getFileName()
                )
                .build()
        );

        headers.setContentLength(
            photo.getSizeBytes()
        );

        return ResponseEntity
            .ok()
            .headers(headers)
            .body(
                photo.getContent()
            );
    }
}
