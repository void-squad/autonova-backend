package com.autonova.payments_billing_service.pdf;

import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class InvoicePdfService {

    private static final Logger log = LoggerFactory.getLogger(InvoicePdfService.class);
    private static final Currency DEFAULT_CURRENCY = Currency.getInstance("LKR");
    private static final String TEMPLATE_BASE_URI = resolveTemplateBaseUri();
    private final SpringTemplateEngine templateEngine;
    private static final DateTimeFormatter INVOICE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final ZoneId INVOICE_ZONE = ZoneId.systemDefault();

    public InvoicePdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] renderInvoice(InvoiceEntity invoice, String paymentMethod) {
        Context context = new Context();
        context.setVariable("invoiceId", invoice.getId());
        context.setVariable("projectId", invoice.getProjectId());
        context.setVariable("projectName", invoice.getProjectName());
        context.setVariable("projectDescription", invoice.getProjectDescription());
        context.setVariable("status", invoice.getStatus().name());
        context.setVariable("issueDate", formatDate(invoice.getCreatedAt()));
        context.setVariable("customerName", invoice.getCustomerEmail());
        context.setVariable("customerEmail", invoice.getCustomerEmail());

        Currency currency = resolveCurrency(invoice.getCurrency());
        context.setVariable("amountDueFormatted", formatCurrency(invoice.getAmountTotal(), currency));
        context.setVariable("currencyCode", currency.getCurrencyCode());
        context.setVariable("paymentMethod", formatPaymentMethod(paymentMethod));

        String html = templateEngine.process("invoice", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ConverterProperties properties = createConverterProperties();
            HtmlConverter.convertToPdf(html, outputStream, properties);
            return outputStream.toByteArray();
        } catch (IOException | RuntimeException e) {
            log.error("Failed to render invoice PDF for invoice {}", invoice.getId(), e);
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }

    private ConverterProperties createConverterProperties() {
        ConverterProperties properties = new ConverterProperties();
        properties.setBaseUri(TEMPLATE_BASE_URI);
        return properties;
    }

    private String formatCurrency(long amountMinorUnits, Currency currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.ENGLISH);
        format.setCurrency(currency);
        return format.format(amountMinorUnits / 100.0);
    }

    private static String resolveTemplateBaseUri() {
        URL baseResource = InvoicePdfService.class.getResource("/templates/");
        if (baseResource == null) {
            log.warn("Unable to locate templates base URI; relative resources may fail to load in PDFs");
            return null;
        }
        return baseResource.toExternalForm();
    }

    private String formatDate(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return "—";
        }
        return timestamp.atZoneSameInstant(INVOICE_ZONE).format(INVOICE_DATE_FORMATTER);
    }

    private Currency resolveCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        String normalized = currencyCode.trim().toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(normalized);
        } catch (IllegalArgumentException ex) {
            return DEFAULT_CURRENCY;
        }
    }

    private String formatPaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return null;
        }
        String cleaned = paymentMethod.trim().replace('_', ' ').replace('-', ' ');
        String[] parts = cleaned.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(part.substring(0, 1).toUpperCase(Locale.ROOT));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.length() > 0 ? builder.toString() : null;
    }
}
