package com.buildflow.siteops.service;

import com.buildflow.shared.error.BusinessException;
import com.buildflow.siteops.dto.SiteOperationDtos;
import com.buildflow.siteops.entity.DailyReportPhoto;
import com.buildflow.siteops.entity.DailySiteReport;
import com.buildflow.siteops.repository.DailyReportPhotoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class DailyReportPhotoService {

    private static final long MAX_SIZE_BYTES =
        5L * 1024L * 1024L;

    private static final Set<String>
        ALLOWED_MIME_TYPES =
            Set.of(
                "image/jpeg",
                "image/png",
                "image/webp"
            );

    private static final Set<String>
        ALLOWED_EXTENSIONS =
            Set.of(
                ".jpg",
                ".jpeg",
                ".png",
                ".webp"
            );

    private final DailyReportPhotoRepository
        photoRepository;

    private final DailySiteReportService
        reportService;

    private final SiteOperationsAccessService
        accessService;

    public DailyReportPhotoService(
        DailyReportPhotoRepository
            photoRepository,
        DailySiteReportService
            reportService,
        SiteOperationsAccessService
            accessService
    ) {
        this.photoRepository =
            photoRepository;

        this.reportService =
            reportService;

        this.accessService =
            accessService;
    }

    @Transactional(readOnly = true)
    public List<SiteOperationDtos.PhotoView> list(
        UUID organizationId,
        UUID userId,
        UUID reportId
    ) {
        DailySiteReport report =
            reportService.requireReport(
                organizationId,
                reportId
            );

        accessService.requireView(
            organizationId,
            report.getProjectId(),
            userId
        );

        return photoRepository
            .findByReportIdAndOrganizationIdOrderByCreatedAtAsc(
                reportId,
                organizationId
            )
            .stream()
            .map(this::toView)
            .toList();
    }

    @Transactional
    public SiteOperationDtos.PhotoView upload(
        UUID organizationId,
        UUID userId,
        UUID reportId,
        SiteOperationDtos.UploadPhotoRequest
            request
    ) {
        DailySiteReport report =
            reportService.requireReport(
                organizationId,
                reportId
            );

        accessService.requireManage(
            organizationId,
            report.getProjectId(),
            userId
        );

        String mimeType =
            request.mimeType()
                .trim()
                .toLowerCase(
                    Locale.ROOT
                );

        if (
            !ALLOWED_MIME_TYPES.contains(
                mimeType
            )
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "SITE_PHOTO_MIME_TYPE_INVALID",
                "Only JPEG, PNG and WebP site photos are supported."
            );
        }

        String fileName =
            request.fileName().trim();

        String lowercaseName =
            fileName.toLowerCase(
                Locale.ROOT
            );

        boolean allowedExtension =
            ALLOWED_EXTENSIONS
                .stream()
                .anyMatch(
                    lowercaseName::endsWith
                );

        if (!allowedExtension) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "SITE_PHOTO_EXTENSION_INVALID",
                "The site photo extension is not supported."
            );
        }

        byte[] content =
            decode(
                request.base64Data()
            );

        if (
            content.length == 0
            || content.length
                > MAX_SIZE_BYTES
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "SITE_PHOTO_SIZE_INVALID",
                "A site photo must be between 1 byte and 5 MB."
            );
        }

        DailyReportPhoto photo =
            DailyReportPhoto.create(
                organizationId,
                report.getProjectId(),
                reportId,
                fileName,
                mimeType,
                content,
                request.caption(),
                request.category(),
                request.clientVisible(),
                userId,
                Instant.now()
            );

        photo =
            photoRepository.save(
                photo
            );

        return toView(photo);
    }

    @Transactional(readOnly = true)
    public DailyReportPhoto content(
        UUID organizationId,
        UUID userId,
        UUID photoId
    ) {
        DailyReportPhoto photo =
            photoRepository
                .findByIdAndOrganizationId(
                    photoId,
                    organizationId
                )
                .orElseThrow(
                    () ->
                        new BusinessException(
                            HttpStatus.NOT_FOUND,
                            "SITE_PHOTO_NOT_FOUND",
                            "The requested site photo does not exist."
                        )
                );

        accessService.requireView(
            organizationId,
            photo.getProjectId(),
            userId
        );

        return photo;
    }

    private byte[] decode(
        String base64
    ) {
        String value =
            base64.trim();

        int comma =
            value.indexOf(',');

        if (
            value.startsWith("data:")
            && comma >= 0
        ) {
            value =
                value.substring(
                    comma + 1
                );
        }

        try {
            return Base64
                .getDecoder()
                .decode(value);
        } catch (
            IllegalArgumentException
                invalidBase64
        ) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "SITE_PHOTO_DATA_INVALID",
                "The supplied site photo data is invalid."
            );
        }
    }

    private SiteOperationDtos.PhotoView toView(
        DailyReportPhoto photo
    ) {
        return new SiteOperationDtos.PhotoView(
            photo.getId(),
            photo.getReportId(),
            photo.getStorageKey(),
            photo.getFileName(),
            photo.getMimeType(),
            photo.getSizeBytes(),
            photo.getCaption(),
            photo.getCategory(),
            photo.isClientVisible(),
            photo.getCreatedBy(),
            photo.getCreatedAt()
        );
    }
}
