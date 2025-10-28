package com.autonova.payments_billing_service.api;

import com.autonova.payments_billing_service.api.dto.InvoiceListResponse;
import com.autonova.payments_billing_service.api.dto.InvoiceResponse;
import com.autonova.payments_billing_service.api.dto.PaymentIntentResponse;
import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.pdf.InvoicePdfService;
import com.autonova.payments_billing_service.service.InvoiceFilter;
import com.autonova.payments_billing_service.service.InvoiceService;
import com.autonova.payments_billing_service.service.PaymentService;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private static final int MAX_LIMIT = 100;

    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final InvoicePdfService invoicePdfService;

    public InvoiceController(InvoiceService invoiceService, PaymentService paymentService, InvoicePdfService invoicePdfService) {
        this.invoiceService = invoiceService;
        this.paymentService = paymentService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','MANAGER')")
    public ResponseEntity<InvoiceListResponse> listInvoices(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "projectId", required = false) String projectId,
        @RequestParam(name = "limit", defaultValue = "50") int limit,
        @RequestParam(name = "offset", defaultValue = "0") int offset,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        int sanitizedLimit = Math.min(Math.max(limit, 1), MAX_LIMIT);
        int sanitizedOffset = Math.max(offset, 0);

        InvoiceStatus invoiceStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Unsupported status value: " + status);
            }
        }

        UUID projectUuid = null;
        if (projectId != null && !projectId.isBlank()) {
            try {
                projectUuid = UUID.fromString(projectId);
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("projectId must be a valid UUID");
            }
        }

        Pageable pageable = PageRequest.of(sanitizedOffset, sanitizedLimit);
        Page<InvoiceEntity> page = invoiceService.findInvoices(new InvoiceFilter(invoiceStatus, projectUuid), user, pageable);
        List<InvoiceResponse> items = page.getContent().stream()
            .map(InvoiceResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(new InvoiceListResponse(items, page.getTotalElements(), sanitizedLimit, sanitizedOffset));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','MANAGER')")
    public ResponseEntity<InvoiceResponse> getInvoice(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        return ResponseEntity.ok(InvoiceResponse.fromEntity(invoice));
    }

    @PostMapping("/{id}/payment-intent")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','MANAGER')")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        PaymentIntentResponse response = paymentService.createOrReusePaymentIntent(invoice, user);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','MANAGER')")
    public ResponseEntity<byte[]> getInvoicePdf(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        byte[] pdfBytes = invoicePdfService.renderInvoice(invoice);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("invoice-" + invoice.getId() + ".pdf")
            .build());
        headers.add("X-Invoice-Status", invoice.getStatus().name());
        headers.add("X-Invoice-Currency", invoice.getCurrency());

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }
}
