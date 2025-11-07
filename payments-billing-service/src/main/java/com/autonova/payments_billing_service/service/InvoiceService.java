package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final String ROLE_CUSTOMER = "customer";

    private final InvoiceRepository invoiceRepository;
    private final DomainEventPublisher eventPublisher;

    public InvoiceService(InvoiceRepository invoiceRepository, DomainEventPublisher eventPublisher) {
        this.invoiceRepository = invoiceRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public Page<InvoiceEntity> findInvoices(InvoiceFilter filter, AuthenticatedUser user, Pageable pageable) {
        Specification<InvoiceEntity> specification = Specification.where(null);

        if (filter.status() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), filter.status()));
        }
        if (filter.projectId() != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("projectId"), filter.projectId()));
        }
        if (filter.hasSearchTerm()) {
            String normalizedSearch = filter.searchTerm().trim().toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", "%");
            String likePattern = "%" + normalizedSearch + "%";
            specification = specification.and((root, query, cb) -> {
                var projectNameExpr = cb.lower(cb.coalesce(root.get("projectName"), ""));
                var projectDescriptionExpr = cb.lower(cb.coalesce(root.get("projectDescription"), ""));
                return cb.or(
                    cb.like(projectNameExpr, likePattern),
                    cb.like(projectDescriptionExpr, likePattern)
                );
            });
        }
        if (user.hasRole(ROLE_CUSTOMER)) {
            String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase(Locale.ROOT) : null;
            if (normalizedEmail == null || normalizedEmail.isEmpty()) {
                throw new AccessDeniedException("Customers must have an email principal");
            }
            specification = specification.and((root, query, cb) -> cb.equal(root.get("customerEmail"), normalizedEmail));
        }

        return invoiceRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public InvoiceEntity getInvoiceForUser(UUID invoiceId, AuthenticatedUser user) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (user.hasRole(ROLE_CUSTOMER)) {
            String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase(Locale.ROOT) : null;
            if (normalizedEmail == null || normalizedEmail.isEmpty()) {
                throw new AccessDeniedException("Customers must have an email principal");
            }
            if (!invoice.getCustomerEmail().equals(normalizedEmail)) {
                throw new AccessDeniedException("Customers may only view their own invoices");
            }
        }

        return invoice;
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceEntity> findById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    @Transactional
    public InvoiceEntity createInvoice(CreateInvoiceCommand command, AuthenticatedUser user) {
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(user, "user");
        Objects.requireNonNull(command.projectId(), "projectId");

        Optional<InvoiceEntity> existing = invoiceRepository.findByProjectId(command.projectId());
        if (existing.isPresent()) {
            throw new IllegalStateException("An invoice already exists for project " + command.projectId());
        }

        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setProjectId(command.projectId());
        invoice.setQuoteId(command.quoteId());
        invoice.setProjectName(command.projectName());
        invoice.setProjectDescription(command.projectDescription());
        String normalizedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase(Locale.ROOT) : null;
        if (normalizedEmail == null || normalizedEmail.isEmpty()) {
            throw new IllegalStateException("Authenticated user is missing email");
        }
        invoice.setCustomerEmail(normalizedEmail);
        invoice.setCustomerUserId(user.getUserId());
        invoice.setCurrency(normalizeCurrency(command.currency()));
        invoice.setAmountTotal(command.amountTotal());
        invoice.setStatus(InvoiceStatus.OPEN);

        invoiceRepository.save(invoice);
        eventPublisher.publishInvoiceCreated(invoice);
        log.info("Created invoice {} for project {} by {}", invoice.getId(), command.projectId(), user.getEmail());
        return invoice;
    }

    @Transactional
    public void markInvoicePaid(InvoiceEntity invoice) {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            eventPublisher.publishInvoiceUpdated(invoice);
        }
    }

    private String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return "lkr";
        }
        return currency.trim().toLowerCase(Locale.ROOT);
    }
}
