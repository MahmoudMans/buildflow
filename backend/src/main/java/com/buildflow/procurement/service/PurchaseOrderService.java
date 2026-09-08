package com.buildflow.procurement.service;

import com.buildflow.organization.entity.OrganizationMembership;
import com.buildflow.organization.service.OrganizationAccessService;
import com.buildflow.procurement.dto.ProcurementDtos;
import com.buildflow.procurement.entity.GoodsReceipt;
import com.buildflow.procurement.entity.GoodsReceiptItem;
import com.buildflow.procurement.entity.PurchaseOrder;
import com.buildflow.procurement.entity.PurchaseOrderItem;
import com.buildflow.procurement.entity.PurchaseOrderStatus;
import com.buildflow.procurement.repository.GoodsReceiptItemRepository;
import com.buildflow.procurement.repository.GoodsReceiptRepository;
import com.buildflow.procurement.repository.PurchaseOrderItemRepository;
import com.buildflow.procurement.repository.PurchaseOrderRepository;
import com.buildflow.shared.error.BusinessException;
import com.buildflow.supplier.entity.Supplier;
import com.buildflow.supplier.service.SupplierService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderRepository
        purchaseOrderRepository;

    private final PurchaseOrderItemRepository
        itemRepository;

    private final GoodsReceiptRepository
        receiptRepository;

    private final GoodsReceiptItemRepository
        receiptItemRepository;

    private final SupplierService
        supplierService;

    private final ProcurementAccessService
        accessService;

    private final PurchaseOrderNumberService
        numberService;

    private final OrganizationAccessService
        organizationAccessService;

    public PurchaseOrderService(
        PurchaseOrderRepository
            purchaseOrderRepository,
        PurchaseOrderItemRepository
            itemRepository,
        GoodsReceiptRepository
            receiptRepository,
        GoodsReceiptItemRepository
            receiptItemRepository,
        SupplierService supplierService,
        ProcurementAccessService
            accessService,
        PurchaseOrderNumberService
            numberService,
        OrganizationAccessService
            organizationAccessService
    ) {
        this.purchaseOrderRepository =
            purchaseOrderRepository;

        this.itemRepository =
            itemRepository;

        this.receiptRepository =
            receiptRepository;

        this.receiptItemRepository =
            receiptItemRepository;

        this.supplierService =
            supplierService;

        this.accessService =
            accessService;

        this.numberService =
            numberService;

        this.organizationAccessService =
            organizationAccessService;
    }

    @Transactional(readOnly = true)
    public List<
        ProcurementDtos.PurchaseOrderSummary
    > list(
        UUID organizationId,
        UUID userId,
        UUID projectId
    ) {
        accessService.requireView(
            organizationId,
            projectId,
            userId
        );

        return purchaseOrderRepository
            .findByProjectIdAndOrganizationIdOrderByCreatedAtDesc(
                projectId,
                organizationId
            )
            .stream()
            .map(
                order ->
                    toSummary(
                        organizationId,
                        order
                    )
            )
            .toList();
    }

    @Transactional(readOnly = true)
    public ProcurementDtos.PurchaseOrderView
        get(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireView(
            organizationId,
            order.getProjectId(),
            userId
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        create(
            UUID organizationId,
            UUID userId,
            UUID projectId,
            ProcurementDtos
                .CreatePurchaseOrderRequest
                request
        ) {

        OrganizationMembership actor =
            accessService.requireManage(
                organizationId,
                projectId,
                userId
            );

        Supplier supplier =
            supplierService
                .requireActiveSupplier(
                    organizationId,
                    request.supplierId()
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

        int year =
            issueDate.getYear();

        PurchaseOrder order =
            PurchaseOrder.create(
                organizationId,
                projectId,
                supplier.getId(),
                numberService.nextNumber(
                    organizationId,
                    year
                ),
                issueDate,
                request.expectedDeliveryDate(),
                clean(
                    request.notes()
                ),
                userId,
                Instant.now()
            );

        purchaseOrderRepository.save(
            order
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        update(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId,
            ProcurementDtos
                .UpdatePurchaseOrderRequest
                request
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        if (
            request.supplierId()
                != null
        ) {
            supplierService
                .requireActiveSupplier(
                    organizationId,
                    request.supplierId()
                );
        }

        order.updateDraft(
            request.supplierId(),
            request.issueDate(),
            request.expectedDeliveryDate(),
            cleanPreserveNull(
                request.notes()
            ),
            Instant.now()
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        addItem(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId,
            ProcurementDtos.CreateItemRequest
                request
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        order.requireDraft();

        PurchaseOrderItem item =
            PurchaseOrderItem.create(
                organizationId,
                order.getProjectId(),
                order.getId(),
                request
                    .description()
                    .trim(),
                request.quantity(),
                request.unit().trim(),
                request.unitPrice(),
                request.taxRate(),
                Instant.now()
            );

        itemRepository.save(item);

        itemRepository.flush();

        recalculateTotals(
            order
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        updateItem(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId,
            UUID itemId,
            ProcurementDtos.UpdateItemRequest
                request
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        order.requireDraft();

        PurchaseOrderItem item =
            requireItem(
                organizationId,
                purchaseOrderId,
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

        recalculateTotals(
            order
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        deleteItem(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId,
            UUID itemId
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        order.requireDraft();

        PurchaseOrderItem item =
            requireItem(
                organizationId,
                purchaseOrderId,
                itemId
            );

        itemRepository.delete(item);

        itemRepository.flush();

        recalculateTotals(
            order
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        submit(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        if (
            itemRepository
                .countByPurchaseOrderIdAndOrganizationId(
                    purchaseOrderId,
                    organizationId
                )
                == 0
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PURCHASE_ORDER_HAS_NO_ITEMS",
                "A purchase order must contain at least one item before submission."
            );
        }

        order.submit(
            Instant.now()
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        approve(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireApproval(
            organizationId,
            order.getProjectId(),
            userId
        );

        order.approve(
            userId,
            Instant.now()
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        markOrdered(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        order.order(
            Instant.now()
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        cancel(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId,
            ProcurementDtos
                .CancelPurchaseOrderRequest
                request
        ) {

        PurchaseOrder order =
            requireOrder(
                organizationId,
                purchaseOrderId
            );

        accessService.requireManage(
            organizationId,
            order.getProjectId(),
            userId
        );

        if (
            receiptRepository
                .existsByPurchaseOrderIdAndOrganizationId(
                    purchaseOrderId,
                    organizationId
                )
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PURCHASE_ORDER_HAS_RECEIPTS",
                "A purchase order with received goods cannot be cancelled."
            );
        }

        order.cancel(
            request.reason(),
            Instant.now()
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional
    public ProcurementDtos.PurchaseOrderView
        receive(
            UUID organizationId,
            UUID userId,
            UUID purchaseOrderId,
            ProcurementDtos
                .CreateGoodsReceiptRequest
                request
        ) {

        PurchaseOrder order =
            purchaseOrderRepository
                .findForUpdate(
                    purchaseOrderId,
                    organizationId
                )
                .orElseThrow(
                    this::purchaseOrderNotFound
                );

        OrganizationMembership receiver =
            accessService.requireReceipt(
                organizationId,
                order.getProjectId(),
                userId
            );

        if (
            order.getStatus()
                != PurchaseOrderStatus.ORDERED
            && order.getStatus()
                != PurchaseOrderStatus.PARTIALLY_RECEIVED
        ) {
            throw new BusinessException(
                HttpStatus.CONFLICT,
                "PURCHASE_ORDER_NOT_RECEIVABLE",
                "Goods can only be received against an ordered purchase order."
            );
        }

        LocalDate receivedDate =
            request.receivedDate();

        if (receivedDate == null) {
            ZoneId zone =
                ZoneId.of(
                    receiver
                        .getOrganization()
                        .getTimezone()
                );

            receivedDate =
                LocalDate.now(zone);
        }

        GoodsReceipt receipt =
            GoodsReceipt.create(
                organizationId,
                order.getProjectId(),
                order.getId(),
                receivedDate,
                receiver.getId(),
                clean(
                    request.notes()
                ),
                Instant.now()
            );

        receiptRepository.save(
            receipt
        );

        for (
            ProcurementDtos.ReceiptItemRequest
                receiptItemRequest
            : request.items()
        ) {
            PurchaseOrderItem orderItem =
                requireItem(
                    organizationId,
                    order.getId(),
                    receiptItemRequest
                        .purchaseOrderItemId()
                );

            BigDecimal alreadyReceived =
                receivedQuantity(
                    organizationId,
                    orderItem.getId()
                );

            BigDecimal requested =
                receiptItemRequest
                    .quantityReceived();

            BigDecimal remaining =
                orderItem
                    .getQuantity()
                    .subtract(
                        alreadyReceived
                    );

            if (
                requested.signum() <= 0
                || requested.compareTo(
                    remaining
                ) > 0
            ) {
                throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RECEIPT_QUANTITY_EXCEEDS_REMAINING",
                    "Received quantity cannot exceed the remaining ordered quantity."
                );
            }

            receiptItemRepository.save(
                GoodsReceiptItem.create(
                    organizationId,
                    order.getProjectId(),
                    order.getId(),
                    receipt.getId(),
                    orderItem.getId(),
                    requested
                )
            );
        }

        receiptItemRepository.flush();

        boolean fullyReceived =
            allItemsReceived(
                organizationId,
                order
            );

        order.updateReceiptStatus(
            fullyReceived,
            Instant.now()
        );

        return toView(
            organizationId,
            order
        );
    }

    @Transactional(readOnly = true)
    public PurchaseOrder requireOrder(
        UUID organizationId,
        UUID purchaseOrderId
    ) {
        return purchaseOrderRepository
            .findByIdAndOrganizationId(
                purchaseOrderId,
                organizationId
            )
            .orElseThrow(
                this::purchaseOrderNotFound
            );
    }

    private PurchaseOrderItem requireItem(
        UUID organizationId,
        UUID purchaseOrderId,
        UUID itemId
    ) {
        return itemRepository
            .findByIdAndPurchaseOrderIdAndOrganizationId(
                itemId,
                purchaseOrderId,
                organizationId
            )
            .orElseThrow(
                () ->
                    new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "PURCHASE_ORDER_ITEM_NOT_FOUND",
                        "The purchase order item does not exist."
                    )
            );
    }

    private void recalculateTotals(
        PurchaseOrder order
    ) {
        List<PurchaseOrderItem> items =
            itemRepository
                .findByPurchaseOrderIdAndOrganizationIdOrderByCreatedAtAsc(
                    order.getId(),
                    order.getOrganizationId()
                );

        BigDecimal subtotal =
            BigDecimal.ZERO;

        BigDecimal tax =
            BigDecimal.ZERO;

        BigDecimal total =
            BigDecimal.ZERO;

        for (
            PurchaseOrderItem item
            : items
        ) {
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

        order.updateTotals(
            subtotal,
            tax,
            total,
            Instant.now()
        );
    }

    private boolean allItemsReceived(
        UUID organizationId,
        PurchaseOrder order
    ) {
        List<PurchaseOrderItem> items =
            itemRepository
                .findByPurchaseOrderIdAndOrganizationIdOrderByCreatedAtAsc(
                    order.getId(),
                    organizationId
                );

        if (items.isEmpty()) {
            return false;
        }

        for (
            PurchaseOrderItem item
            : items
        ) {
            BigDecimal received =
                receivedQuantity(
                    organizationId,
                    item.getId()
                );

            if (
                received.compareTo(
                    item.getQuantity()
                ) < 0
            ) {
                return false;
            }
        }

        return true;
    }

    private BigDecimal receivedQuantity(
        UUID organizationId,
        UUID purchaseOrderItemId
    ) {
        BigDecimal received =
            receiptItemRepository
                .sumReceivedQuantity(
                    purchaseOrderItemId,
                    organizationId
                );

        if (received == null) {
            return BigDecimal.ZERO
                .setScale(4);
        }

        return received.setScale(
            4,
            RoundingMode.HALF_UP
        );
    }

    private ProcurementDtos.PurchaseOrderSummary
        toSummary(
            UUID organizationId,
            PurchaseOrder order
        ) {

        Supplier supplier =
            supplierService
                .requireSupplier(
                    organizationId,
                    order.getSupplierId()
                );

        return new ProcurementDtos
            .PurchaseOrderSummary(
                order.getId(),
                order.getPurchaseOrderNumber(),
                supplier.getId(),
                supplier.getName(),
                order.getStatus(),
                order.getIssueDate(),
                order.getExpectedDeliveryDate(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTotal(),
                order.getCreatedAt()
            );
    }

    private ProcurementDtos.PurchaseOrderView
        toView(
            UUID organizationId,
            PurchaseOrder order
        ) {

        Supplier supplier =
            supplierService
                .requireSupplier(
                    organizationId,
                    order.getSupplierId()
                );

        List<
            ProcurementDtos.PurchaseOrderItemView
        > items =
            itemRepository
                .findByPurchaseOrderIdAndOrganizationIdOrderByCreatedAtAsc(
                    order.getId(),
                    organizationId
                )
                .stream()
                .map(
                    item -> {
                        BigDecimal received =
                            receivedQuantity(
                                organizationId,
                                item.getId()
                            );

                        BigDecimal remaining =
                            item
                                .getQuantity()
                                .subtract(
                                    received
                                );

                        if (
                            remaining.signum()
                                < 0
                        ) {
                            remaining =
                                BigDecimal.ZERO;
                        }

                        return new ProcurementDtos
                            .PurchaseOrderItemView(
                                item.getId(),
                                item.getDescription(),
                                item.getQuantity(),
                                item.getUnit(),
                                item.getUnitPrice(),
                                item.getTaxRate(),
                                item.getLineSubtotal(),
                                item.getTaxAmount(),
                                item.getLineTotal(),
                                received,
                                remaining
                            );
                    }
                )
                .toList();

        List<
            ProcurementDtos.GoodsReceiptView
        > receipts =
            receiptRepository
                .findByPurchaseOrderIdAndOrganizationIdOrderByReceivedDateDescCreatedAtDesc(
                    order.getId(),
                    organizationId
                )
                .stream()
                .map(
                    receipt ->
                        receiptView(
                            organizationId,
                            order,
                            receipt
                        )
                )
                .toList();

        return new ProcurementDtos
            .PurchaseOrderView(
                order.getId(),
                order.getProjectId(),
                supplier.getId(),
                supplier.getName(),
                order.getPurchaseOrderNumber(),
                order.getStatus(),
                order.getIssueDate(),
                order.getExpectedDeliveryDate(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getTotal(),
                order.getNotes(),
                order.getCreatedBy(),
                order.getApprovedBy(),
                order.getApprovedAt(),
                order.getOrderedAt(),
                order.getCancelledAt(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                items,
                receipts
            );
    }

    private ProcurementDtos.GoodsReceiptView
        receiptView(
            UUID organizationId,
            PurchaseOrder order,
            GoodsReceipt receipt
        ) {

        List<
            ProcurementDtos.GoodsReceiptItemView
        > items =
            receiptItemRepository
                .findByGoodsReceiptId(
                    receipt.getId()
                )
                .stream()
                .map(
                    receiptItem -> {
                        PurchaseOrderItem
                            orderItem =
                                requireItem(
                                    organizationId,
                                    order.getId(),
                                    receiptItem
                                        .getPurchaseOrderItemId()
                                );

                        return new ProcurementDtos
                            .GoodsReceiptItemView(
                                orderItem.getId(),
                                orderItem.getDescription(),
                                receiptItem
                                    .getQuantityReceived()
                            );
                    }
                )
                .toList();

        return new ProcurementDtos
            .GoodsReceiptView(
                receipt.getId(),
                receipt.getReceivedDate(),
                receipt
                    .getReceivedByMembershipId(),
                receipt.getNotes(),
                receipt.getCreatedAt(),
                items
            );
    }

    private BusinessException
        purchaseOrderNotFound() {

        return new BusinessException(
            HttpStatus.NOT_FOUND,
            "PURCHASE_ORDER_NOT_FOUND",
            "The purchase order does not exist or is not accessible."
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
