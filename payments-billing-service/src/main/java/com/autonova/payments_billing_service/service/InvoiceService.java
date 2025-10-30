package com.autonova.payments_billing_service.service;

import com.autonova.payments_billing_service.auth.AuthenticatedUser;
import com.autonova.payments_billing_service.domain.ConsumedEventEntity;
import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.autonova.payments_billing_service.domain.InvoiceStatus;
import com.autonova.payments_billing_service.events.QuoteApprovedEvent;
import com.autonova.payments_billing_service.messaging.DomainEventPublisher;
import com.autonova.payments_billing_service.repository.ConsumedEventRepository;
import com.autonova.payments_billing_service.repository.InvoiceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final ConsumedEventRepository consumedEventRepository;
    private final DomainEventPublisher eventPublisher;

    public InvoiceService(
        InvoiceRepository invoiceRepository,
        ConsumedEventRepository consumedEventRepository,
        DomainEventPublisher eventPublisher
    ) {
        this.invoiceRepository = invoiceRepository;
        this.consumedEventRepository = consumedEventRepository;
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
        if (user.hasRole(ROLE_CUSTOMER)) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("customerId"), user.getUserId()));
        }

        return invoiceRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public InvoiceEntity getInvoiceForUser(UUID invoiceId, AuthenticatedUser user) {
        InvoiceEntity invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new EntityNotFoundException("Invoice not found: " + invoiceId));

        if (user.hasRole(ROLE_CUSTOMER) && !invoice.getCustomerId().equals(user.getUserId())) {
            throw new AccessDeniedException("Customers may only view their own invoices");
        }

        return invoice;
    }

    @Transactional(readOnly = true)
    public Optional<InvoiceEntity> findById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId);
    }

    @Transactional
    public void handleQuoteApproved(QuoteApprovedEvent event) {
        if (!claimEvent(event.id(), event.type())) {
            return;
        }

        QuoteApprovedEvent.QuoteApprovedData data = event.data();
        if (data == null) {
            log.warn("quote.approved event missing data payload: {}", event);
            return;
        }

        Optional<InvoiceEntity> existing = invoiceRepository.findByProjectId(data.projectId());
        if (existing.isEmpty()) {
            InvoiceEntity invoice = new InvoiceEntity();
            invoice.setId(UUID.randomUUID());
            invoice.setProjectId(data.projectId());
            invoice.setCustomerId(data.customerId());
            invoice.setQuoteId(data.quoteId());
            invoice.setCurrency(data.currency() == null ? "LKR" : data.currency().toUpperCase());
            invoice.setAmountTotal(data.total());
            invoice.setStatus(InvoiceStatus.OPEN);
            invoiceRepository.save(invoice);
            eventPublisher.publishInvoiceCreated(invoice);
            log.info("Created invoice {} for project {}", invoice.getId(), data.projectId());
            return;
        }

        InvoiceEntity invoice = existing.get();
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.VOID) {
            log.info("Skipping invoice {} update because status is {}", invoice.getId(), invoice.getStatus());
            return;
        }

        long previousAmount = invoice.getAmountTotal();
        invoice.setAmountTotal(data.total());
        if (data.currency() != null) {
            invoice.setCurrency(data.currency().toUpperCase());
        }
        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            invoice.setStatus(InvoiceStatus.OPEN);
        }
        invoiceRepository.save(invoice);

        if (previousAmount != data.total()) {
            eventPublisher.publishInvoiceUpdated(invoice);
        }
    }

    @Transactional
    public void markInvoicePaid(InvoiceEntity invoice) {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoiceRepository.save(invoice);
            eventPublisher.publishInvoiceUpdated(invoice);
        }
    }

    private boolean claimEvent(UUID eventId, String type) {
        try {
            consumedEventRepository.save(new ConsumedEventEntity(eventId, type, OffsetDateTime.now()));
            return true;
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Event {} already processed", eventId);
            return false;
        }
    }
}
