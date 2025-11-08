package com.autonova.payments_billing_service.api;

import com.autonova.payments_billing_service.api.dto.InvoiceCreateRequest;
import com.autonova.payments_billing_service.api.dto.InvoiceListResponse;
import com.autonova.payments_billing_service.api.dto.InvoiceResponse;
import com.autonova.payments_billing_service.api.dto.PaymentIntentResponse;
import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.domain.PaymentProvider;
import com.autonova.payments_billing_service.pdf.InvoicePdfService;
import com.autonova.payments_billing_service.service.CreateInvoiceCommand;
import com.autonova.payments_billing_service.service.InvoiceFilter;
import com.autonova.payments_billing_service.service.InvoiceService;
import com.autonova.payments_billing_service.service.PaymentService;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

@RestController
@RequestMapping("/api/invoices")
@Validated
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
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<InvoiceListResponse> listInvoices(
        @RequestParam(name = "status", required = false) String status,
        @RequestParam(name = "projectId", required = false) String projectId,
        @RequestParam(name = "search", required = false) String search,
        @RequestParam(name = "limit", defaultValue = "50") @Min(1) @Max(MAX_LIMIT) int limit,
        @RequestParam(name = "offset", defaultValue = "0") @PositiveOrZero int offset,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
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

        String searchQuery = null;
        if (search != null && !search.isBlank()) {
            searchQuery = search.trim();
        }

        int pageNumber = offset / limit;
        int effectiveOffset = pageNumber * limit;

        Pageable pageable = PageRequest.of(pageNumber, limit);
        Page<InvoiceEntity> page = invoiceService.findInvoices(
            new InvoiceFilter(invoiceStatus, projectUuid, searchQuery),
            user,
            pageable
        );
        List<InvoiceResponse> items = page.getContent().stream()
            .map(this::buildInvoiceResponse)
            .toList();

        return ResponseEntity.ok(new InvoiceListResponse(items, page.getTotalElements(), limit, effectiveOffset));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<InvoiceResponse> getInvoice(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        return ResponseEntity.ok(buildInvoiceResponse(invoice));
    }

    @PostMapping("/{id}/payment-intent")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        PaymentIntentResponse response = paymentService.createOrReusePaymentIntent(invoice, user);
        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAnyRole('EMPLOYEE','ADMIN')")
    public ResponseEntity<InvoiceResponse> markInvoicePaid(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        paymentService.recordOfflinePayment(invoice, user);
        return ResponseEntity.ok(buildInvoiceResponse(invoice));
    }

    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAnyRole('CUSTOMER','EMPLOYEE','ADMIN')")
    public ResponseEntity<byte[]> getInvoicePdf(
        @PathVariable("id") UUID invoiceId,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        InvoiceEntity invoice = invoiceService.getInvoiceForUser(invoiceId, user);
        String paymentMethodLabel = resolvePaymentMethodLabel(invoice);
        byte[] pdfBytes = invoicePdfService.renderInvoice(invoice, paymentMethodLabel);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
            .filename("invoice-" + invoice.getId() + ".pdf")
            .build());
        headers.add("X-Invoice-Status", invoice.getStatus().name());
        String currencyHeader = invoice.getCurrency() != null ? invoice.getCurrency().toUpperCase(Locale.ROOT) : null;
        if (currencyHeader != null) {
            headers.add("X-Invoice-Currency", currencyHeader);
        }

        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfBytes);
    }

    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<InvoiceResponse> createInvoice(
        @Valid @RequestBody InvoiceCreateRequest request,
        @AuthenticationPrincipal AuthenticatedUser user
    ) {
        CreateInvoiceCommand command = new CreateInvoiceCommand(
            request.projectId(),
            request.quoteId(),
            request.projectName(),
            request.projectDescription(),
            request.amountTotal(),
            request.currency()
        );
        InvoiceEntity invoice = invoiceService.createInvoice(command, user);
        return ResponseEntity.status(201).body(buildInvoiceResponse(invoice));
    }

    private InvoiceResponse buildInvoiceResponse(InvoiceEntity invoice) {
        return InvoiceResponse.fromEntity(invoice, resolvePaymentMethodLabel(invoice));
    }

    private String resolvePaymentMethodLabel(InvoiceEntity invoice) {
        return paymentService.findLatestSuccessfulPaymentProvider(invoice.getId())
            .map(this::formatPaymentProvider)
            .orElse(null);
    }

    private String formatPaymentProvider(PaymentProvider provider) {
        String lower = provider.name().toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
