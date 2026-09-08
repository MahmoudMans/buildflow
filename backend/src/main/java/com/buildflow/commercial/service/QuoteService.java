package com.buildflow.commercial.service;

import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.entity.Quote;
import com.buildflow.commercial.entity.QuoteItem;
import com.buildflow.commercial.repository.QuoteItemRepository;
import com.buildflow.commercial.repository.QuoteRepository;
import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.shared.error.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final QuoteItemRepository itemRepository;
    private final CommercialAccessService accessService;
    private final CommercialNumberService numberService;
    private final ProjectCommercialDataService projectDataService;

    public QuoteService(
        QuoteRepository quoteRepository,
        QuoteItemRepository itemRepository,
        CommercialAccessService accessService,
        CommercialNumberService numberService,
        ProjectCommercialDataService projectDataService
    ) {
        this.quoteRepository = quoteRepository;
        this.itemRepository = itemRepository;
        this.accessService = accessService;
        this.numberService = numberService;
        this.projectDataService =
            projectDataService;
    }

    @Transactional(readOnly = true)
    public List<CommercialDtos.QuoteSummary> list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return quoteRepository
            .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public CommercialDtos.QuoteView get(
        UUID organizationId,
        UUID userId,
        UUID quoteId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireView(
            organizationId,
            quote.getProjectId(),
            userId
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        CommercialDtos.CreateQuoteRequest request
    ) {
        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                projectId,
                userId
            );

        ZoneId zone =
            ZoneId.of(
                actor
                    .getOrganization()
                    .getTimezone()
            );

        LocalDate issueDate =
            request.issueDate() == null
                ? LocalDate.now(zone)
                : request.issueDate();

        Quote quote =
            Quote.create(
                organizationId,
                projectId,
                projectDataService.clientId(
                    organizationId,
                    projectId
                ),
                numberService.nextQuoteNumber(
                    organizationId,
                    issueDate.getYear()
                ),
                issueDate,
                request.validUntil(),
                clean(request.notes()),
                clean(request.terms()),
                request.contractQuote(),
                userId,
                Instant.now()
            );

        quote =
            quoteRepository.save(
                quote
            );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView update(
        UUID organizationId,
        UUID userId,
        UUID quoteId,
        CommercialDtos.UpdateQuoteRequest request
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireManage(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.updateDraft(
            request.issueDate(),
            request.validUntil(),
            cleanPreserveNull(
                request.notes()
            ),
            cleanPreserveNull(
                request.terms()
            ),
            request.contractQuote(),
            Instant.now()
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView addItem(
        UUID organizationId,
        UUID userId,
        UUID quoteId,
        CommercialDtos.LineRequest request
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireManage(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.requireFinanciallyEditable();

        QuoteItem item =
            QuoteItem.create(
                organizationId,
                quote.getProjectId(),
                quote.getId(),
                request.description().trim(),
                request.quantity(),
                request.unit().trim(),
                request.unitPrice(),
                request.taxRate(),
                Instant.now()
            );

        itemRepository.save(item);
        itemRepository.flush();

        recalculate(quote);

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView updateItem(
        UUID organizationId,
        UUID userId,
        UUID quoteId,
        UUID itemId,
        CommercialDtos.UpdateLineRequest request
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireManage(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.requireFinanciallyEditable();

        QuoteItem item =
            requireItem(
                organizationId,
                quoteId,
                itemId
            );

        item.update(
            cleanPreserveNull(
                request.description()
            ),
            request.quantity(),
            cleanPreserveNull(
                request.unit()
            ),
            request.unitPrice(),
            request.taxRate(),
            Instant.now()
        );

        itemRepository.flush();

        recalculate(quote);

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView deleteItem(
        UUID organizationId,
        UUID userId,
        UUID quoteId,
        UUID itemId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireManage(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.requireFinanciallyEditable();

        itemRepository.delete(
            requireItem(
                organizationId,
                quoteId,
                itemId
            )
        );

        itemRepository.flush();

        recalculate(quote);

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView send(
        UUID organizationId,
        UUID userId,
        UUID quoteId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                quote.getProjectId(),
                userId
            );

        if (
            itemRepository
                .countByQuotationIdAndOrganizationId(
                    quoteId,
                    organizationId
                )
                == 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_HAS_NO_LINES",
                "A quotation must contain at least one line before it can be sent."
            );
        }

        quote.send(
            today(actor),
            Instant.now()
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView markViewed(
        UUID organizationId,
        UUID userId,
        UUID quoteId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireView(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.markViewed(
            Instant.now()
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView accept(
        UUID organizationId,
        UUID userId,
        UUID quoteId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        OrganizationMembership actor =
            accessService.requireDecision(
                organizationId,
                quote.getProjectId(),
                userId
            );

        quote.accept(
            today(actor),
            userId,
            Instant.now()
        );

        if (quote.isContractQuote()) {
            projectDataService
                .establishInitialContractValue(
                    organizationId,
                    quote.getProjectId(),
                    quote.getTotal()
                );
        }

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView reject(
        UUID organizationId,
        UUID userId,
        UUID quoteId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireDecision(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.reject(
            Instant.now()
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView expire(
        UUID organizationId,
        UUID userId,
        UUID quoteId
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                quote.getProjectId(),
                userId
            );

        quote.expire(
            today(actor),
            Instant.now()
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional
    public CommercialDtos.QuoteView cancel(
        UUID organizationId,
        UUID userId,
        UUID quoteId,
        CommercialDtos.CancelRequest request
    ) {
        Quote quote =
            requireQuote(
                organizationId,
                quoteId
            );

        accessService.requireManage(
            organizationId,
            quote.getProjectId(),
            userId
        );

        quote.cancel(
            request.reason().trim(),
            Instant.now()
        );

        return toView(
            organizationId,
            quote
        );
    }

    @Transactional(readOnly = true)
    public Quote requireQuote(
        UUID organizationId,
        UUID quoteId
    ) {
        return quoteRepository
            .findByIdAndOrganizationId(
                quoteId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "QUOTE_NOT_FOUND",
                        "The quotation does not exist or is not accessible."
                    )
            );
    }

    private QuoteItem requireItem(
        UUID organizationId,
        UUID quoteId,
        UUID itemId
    ) {
        return itemRepository
            .findByIdAndQuotationIdAndOrganizationId(
                itemId,
                quoteId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "QUOTE_LINE_NOT_FOUND",
                        "The quotation line does not exist."
                    )
            );
    }

    private void recalculate(
        Quote quote
    ) {
        List<QuoteItem> items =
            itemRepository
                .findByQuotationIdAndOrganizationIdOrderByCreatedAtAsc(
                    quote.getId(),
                    quote.getOrganizationId()
                );

        BigDecimal subtotal =
            BigDecimal.ZERO;

        BigDecimal tax =
            BigDecimal.ZERO;

        BigDecimal total =
            BigDecimal.ZERO;

        for (QuoteItem item : items) {
            subtotal =
                subtotal.add(
                    item.getLineSubtotal()
                );

            tax =
                tax.add(
                    item.getTaxAmount()
                );

            total =
                total.add(
                    item.getLineTotal()
                );
        }

        quote.updateTotals(
            subtotal,
            tax,
            total,
            Instant.now()
        );
    }

    private CommercialDtos.QuoteSummary
        toSummary(
            Quote quote
        ) {

        return new CommercialDtos
            .QuoteSummary(
                quote.getId(),
                quote.getQuoteNumber(),
                quote.getStatus(),
                quote.isContractQuote(),
                quote.getIssueDate(),
                quote.getValidUntil(),
                quote.getTotal(),
                quote.getCreatedAt()
            );
    }

    private CommercialDtos.QuoteView
        toView(
            UUID organizationId,
            Quote quote
        ) {

        List<CommercialDtos.QuoteLineView>
            items =
                itemRepository
                    .findByQuotationIdAndOrganizationIdOrderByCreatedAtAsc(
                        quote.getId(),
                        organizationId
                    )
                    .stream()
                    .map(
                        item ->
                            new CommercialDtos
                                .QuoteLineView(
                                    item.getId(),
                                    item.getDescription(),
                                    item.getQuantity(),
                                    item.getUnit(),
                                    item.getUnitPrice(),
                                    item.getTaxRate(),
                                    item.getLineSubtotal(),
                                    item.getTaxAmount(),
                                    item.getLineTotal()
                                )
                    )
                    .toList();

        return new CommercialDtos
            .QuoteView(
                quote.getId(),
                quote.getProjectId(),
                quote.getClientId(),
                quote.getQuoteNumber(),
                quote.getStatus(),
                quote.isContractQuote(),
                quote.getIssueDate(),
                quote.getValidUntil(),
                quote.getSubtotal(),
                quote.getTaxAmount(),
                quote.getTotal(),
                quote.getNotes(),
                quote.getTerms(),
                quote.getCreatedBy(),
                quote.getSentAt(),
                quote.getViewedAt(),
                quote.getAcceptedAt(),
                quote.getAcceptedBy(),
                quote.getRejectedAt(),
                quote.getExpiredAt(),
                quote.getCancelledAt(),
                quote.getCancellationReason(),
                quote.getCreatedAt(),
                items
            );
    }

    private LocalDate today(
        OrganizationMembership membership
    ) {
        return LocalDate.now(
            ZoneId.of(
                membership
                    .getOrganization()
                    .getTimezone()
            )
        );
    }

    private String clean(
        String value
    ) {
        if (
            value == null
            || value.isBlank()
        ) {
            return null;
        }

        return value.trim();
    }

    private String cleanPreserveNull(
        String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }
}
