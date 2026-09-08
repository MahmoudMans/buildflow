package com.buildflow.commercial.service;

import com.buildflow.commercial.dto.CommercialDtos;
import com.buildflow.commercial.entity.Invoice;
import com.buildflow.commercial.entity.InvoiceItem;
import com.buildflow.commercial.entity.Payment;
import com.buildflow.commercial.entity.Quote;
import com.buildflow.commercial.entity.QuoteItem;
import com.buildflow.commercial.entity.QuoteStatus;
import com.buildflow.commercial.repository.InvoiceItemRepository;
import com.buildflow.commercial.repository.InvoiceRepository;
import com.buildflow.commercial.repository.PaymentRepository;
import com.buildflow.commercial.repository.QuoteItemRepository;
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
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository itemRepository;
    private final PaymentRepository paymentRepository;
    private final QuoteItemRepository quoteItemRepository;
    private final QuoteService quoteService;
    private final CommercialAccessService accessService;
    private final CommercialNumberService numberService;
    private final ProjectCommercialDataService projectDataService;

    public InvoiceService(
        InvoiceRepository invoiceRepository,
        InvoiceItemRepository itemRepository,
        PaymentRepository paymentRepository,
        QuoteItemRepository quoteItemRepository,
        QuoteService quoteService,
        CommercialAccessService accessService,
        CommercialNumberService numberService,
        ProjectCommercialDataService projectDataService
    ) {
        this.invoiceRepository =
            invoiceRepository;

        this.itemRepository =
            itemRepository;

        this.paymentRepository =
            paymentRepository;

        this.quoteItemRepository =
            quoteItemRepository;

        this.quoteService = quoteService;
        this.accessService = accessService;
        this.numberService = numberService;

        this.projectDataService =
            projectDataService;
    }

    @Transactional(readOnly = true)
    public List<CommercialDtos.InvoiceSummary> list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return invoiceRepository
            .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(this::toSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public CommercialDtos.InvoiceView get(
        UUID organizationId,
        UUID userId,
        UUID invoiceId
    ) {
        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireView(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView create(
        UUID organizationId,
        UUID userId,
        UUID projectId,
        CommercialDtos.CreateInvoiceRequest request
    ) {
        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                projectId,
                userId
            );

        LocalDate issueDate =
            request.issueDate() == null
                ? today(actor)
                : request.issueDate();

        Invoice invoice =
            Invoice.create(
                organizationId,
                projectId,
                projectDataService.clientId(
                    organizationId,
                    projectId
                ),
                null,
                numberService.nextInvoiceNumber(
                    organizationId,
                    issueDate.getYear()
                ),
                issueDate,
                request.dueDate(),
                clean(request.notes()),
                clean(
                    request.paymentInstructions()
                ),
                Instant.now()
            );

        invoice =
            invoiceRepository.save(
                invoice
            );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView
        createFromQuote(
            UUID organizationId,
            UUID userId,
            UUID quoteId,
            CommercialDtos
                .CreateInvoiceFromQuoteRequest
                request
        ) {

        Quote quote =
            quoteService.requireQuote(
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
            quote.getStatus()
                != QuoteStatus.ACCEPTED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "QUOTE_NOT_ACCEPTED",
                "Only an accepted quotation can be converted to an invoice."
            );
        }

        LocalDate issueDate =
            request.issueDate() == null
                ? today(actor)
                : request.issueDate();

        Invoice invoice =
            Invoice.create(
                organizationId,
                quote.getProjectId(),
                quote.getClientId(),
                quote.getId(),
                numberService.nextInvoiceNumber(
                    organizationId,
                    issueDate.getYear()
                ),
                issueDate,
                request.dueDate(),
                clean(request.notes()),
                clean(
                    request.paymentInstructions()
                ),
                Instant.now()
            );

        invoice =
            invoiceRepository.save(
                invoice
            );

        List<QuoteItem> quoteItems =
            quoteItemRepository
                .findByQuotationIdAndOrganizationIdOrderByCreatedAtAsc(
                    quote.getId(),
                    organizationId
                );

        for (QuoteItem quoteItem : quoteItems) {
            itemRepository.save(
                InvoiceItem.create(
                    organizationId,
                    quote.getProjectId(),
                    invoice.getId(),
                    quoteItem.getDescription(),
                    quoteItem.getQuantity(),
                    quoteItem.getUnit(),
                    quoteItem.getUnitPrice(),
                    quoteItem.getTaxRate(),
                    Instant.now()
                )
            );
        }

        itemRepository.flush();

        invoice.updateTotals(
            quote.getSubtotal(),
            quote.getTaxAmount(),
            quote.getTotal(),
            Instant.now()
        );

        invoiceRepository.flush();

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView update(
        UUID organizationId,
        UUID userId,
        UUID invoiceId,
        CommercialDtos.UpdateInvoiceRequest request
    ) {
        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireManage(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        invoice.updateDraft(
            request.issueDate(),
            request.dueDate(),
            cleanPreserveNull(
                request.notes()
            ),
            cleanPreserveNull(
                request.paymentInstructions()
            ),
            Instant.now()
        );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView addItem(
        UUID organizationId,
        UUID userId,
        UUID invoiceId,
        CommercialDtos.LineRequest request
    ) {
        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireManage(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        invoice.requireFinanciallyEditable();

        itemRepository.save(
            InvoiceItem.create(
                organizationId,
                invoice.getProjectId(),
                invoice.getId(),
                request.description().trim(),
                request.quantity(),
                request.unit().trim(),
                request.unitPrice(),
                request.taxRate(),
                Instant.now()
            )
        );

        itemRepository.flush();

        recalculate(invoice);

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView
        updateItem(
            UUID organizationId,
            UUID userId,
            UUID invoiceId,
            UUID itemId,
            CommercialDtos.UpdateLineRequest
                request
        ) {

        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireManage(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        invoice.requireFinanciallyEditable();

        InvoiceItem item =
            requireItem(
                organizationId,
                invoiceId,
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

        recalculate(invoice);

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView
        deleteItem(
            UUID organizationId,
            UUID userId,
            UUID invoiceId,
            UUID itemId
        ) {

        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireManage(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        invoice.requireFinanciallyEditable();

        itemRepository.delete(
            requireItem(
                organizationId,
                invoiceId,
                itemId
            )
        );

        itemRepository.flush();

        recalculate(invoice);

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView issue(
        UUID organizationId,
        UUID userId,
        UUID invoiceId
    ) {
        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireManage(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        if (
            itemRepository
                .countByInvoiceIdAndOrganizationId(
                    invoiceId,
                    organizationId
                )
                == 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "INVOICE_HAS_NO_LINES",
                "An invoice must contain at least one line before issue."
            );
        }

        invoice.issue(
            Instant.now()
        );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView
        voidInvoice(
            UUID organizationId,
            UUID userId,
            UUID invoiceId,
            CommercialDtos.VoidInvoiceRequest
                request
        ) {

        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        accessService.requireManage(
            organizationId,
            invoice.getProjectId(),
            userId
        );

        invoice.voidInvoice(
            request.reason(),
            Instant.now()
        );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView
        markOverdue(
            UUID organizationId,
            UUID userId,
            UUID invoiceId
        ) {

        Invoice invoice =
            requireInvoice(
                organizationId,
                invoiceId
            );

        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                invoice.getProjectId(),
                userId
            );

        invoice.markOverdue(
            today(actor),
            Instant.now()
        );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional
    public CommercialDtos.InvoiceView
        recordPayment(
            UUID organizationId,
            UUID userId,
            UUID invoiceId,
            CommercialDtos.RecordPaymentRequest
                request
        ) {

        Invoice invoice =
            invoiceRepository
                .findForUpdate(
                    invoiceId,
                    organizationId
                )
                .orElseThrow(
                    this::invoiceNotFound
                );

        OrganizationMembership actor =
            accessService.requirePayment(
                organizationId,
                invoice.getProjectId(),
                userId
            );

        LocalDate paymentDate =
            request.paymentDate() == null
                ? today(actor)
                : request.paymentDate();

        invoice.applyPayment(
            request.amount(),
            Instant.now()
        );

        paymentRepository.save(
            Payment.create(
                organizationId,
                invoice.getProjectId(),
                invoice.getId(),
                paymentDate,
                request.amount(),
                request.method(),
                clean(request.reference()),
                clean(request.notes()),
                userId,
                Instant.now()
            )
        );

        return toView(
            organizationId,
            invoice
        );
    }

    @Transactional(readOnly = true)
    public Invoice requireInvoice(
        UUID organizationId,
        UUID invoiceId
    ) {
        return invoiceRepository
            .findByIdAndOrganizationId(
                invoiceId,
                organizationId
            )
            .orElseThrow(
                this::invoiceNotFound
            );
    }

    private InvoiceItem requireItem(
        UUID organizationId,
        UUID invoiceId,
        UUID itemId
    ) {
        return itemRepository
            .findByIdAndInvoiceIdAndOrganizationId(
                itemId,
                invoiceId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INVOICE_LINE_NOT_FOUND",
                        "The invoice line does not exist."
                    )
            );
    }

    private void recalculate(
        Invoice invoice
    ) {
        List<InvoiceItem> items =
            itemRepository
                .findByInvoiceIdAndOrganizationIdOrderByCreatedAtAsc(
                    invoice.getId(),
                    invoice.getOrganizationId()
                );

        BigDecimal subtotal =
            BigDecimal.ZERO;

        BigDecimal tax =
            BigDecimal.ZERO;

        BigDecimal total =
            BigDecimal.ZERO;

        for (InvoiceItem item : items) {
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

        invoice.updateTotals(
            subtotal,
            tax,
            total,
            Instant.now()
        );

        invoiceRepository.flush();
    }

    private CommercialDtos.InvoiceSummary
        toSummary(
            Invoice invoice
        ) {

        return new CommercialDtos
            .InvoiceSummary(
                invoice.getId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getTotal(),
                invoice.getAmountPaid(),
                invoice.getAmountOutstanding(),
                invoice.getCreatedAt()
            );
    }

    private CommercialDtos.InvoiceView
        toView(
            UUID organizationId,
            Invoice invoice
        ) {

        List<CommercialDtos.InvoiceLineView>
            items =
                itemRepository
                    .findByInvoiceIdAndOrganizationIdOrderByCreatedAtAsc(
                        invoice.getId(),
                        organizationId
                    )
                    .stream()
                    .map(
                        item ->
                            new CommercialDtos
                                .InvoiceLineView(
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

        List<CommercialDtos.PaymentView>
            payments =
                paymentRepository
                    .findByInvoiceIdAndOrganizationIdOrderByPaymentDateDescCreatedAtDesc(
                        invoice.getId(),
                        organizationId
                    )
                    .stream()
                    .map(
                        payment ->
                            new CommercialDtos
                                .PaymentView(
                                    payment.getId(),
                                    payment.getPaymentDate(),
                                    payment.getAmount(),
                                    payment.getMethod(),
                                    payment.getReference(),
                                    payment.getNotes(),
                                    payment.getCreatedBy(),
                                    payment.getCreatedAt()
                                )
                    )
                    .toList();

        return new CommercialDtos
            .InvoiceView(
                invoice.getId(),
                invoice.getProjectId(),
                invoice.getClientId(),
                invoice.getSourceQuoteId(),
                invoice.getInvoiceNumber(),
                invoice.getStatus(),
                invoice.getIssueDate(),
                invoice.getDueDate(),
                invoice.getSubtotal(),
                invoice.getTaxAmount(),
                invoice.getTotal(),
                invoice.getAmountPaid(),
                invoice.getAmountOutstanding(),
                invoice.getNotes(),
                invoice.getPaymentInstructions(),
                invoice.getIssuedAt(),
                invoice.getVoidedAt(),
                invoice.getVoidReason(),
                invoice.getCreatedAt(),
                items,
                payments
            );
    }

    private BusinessException
        invoiceNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "INVOICE_NOT_FOUND",
            "The invoice does not exist or is not accessible."
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
