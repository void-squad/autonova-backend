package com.autonova.payments_billing_service.pdf;

import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private final SpringTemplateEngine templateEngine;
    private static final DateTimeFormatter INVOICE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final ZoneId INVOICE_ZONE = ZoneId.systemDefault();

    public InvoicePdfService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public byte[] renderInvoice(InvoiceEntity invoice) {
        Context context = new Context();
        context.setVariable("invoiceId", invoice.getId());
        context.setVariable("projectId", invoice.getProjectId());
        context.setVariable("status", invoice.getStatus().name());
        context.setVariable("issueDate", formatDate(invoice.getCreatedAt()));
        context.setVariable("customerName", "Customer");
        context.setVariable("customerEmail", "customer@example.com");

        Currency currency = resolveCurrency(invoice.getCurrency());
        context.setVariable("amountDueFormatted", formatCurrency(invoice.getAmountTotal(), currency));
        context.setVariable("currencyCode", currency.getCurrencyCode());

        String html = templateEngine.process("invoice", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            registerFontIfAvailable(builder, "/fonts/Lato-Regular.ttf", "Lato", 400);
            registerFontIfAvailable(builder, "/fonts/Lato-Bold.ttf", "Lato", 700);
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }

    private void registerFontIfAvailable(PdfRendererBuilder builder, String resourcePath, String familyName, int weight) {
        URL resource = InvoicePdfService.class.getResource(resourcePath);
        if (resource == null) {
            log.debug("Font resource {} not found; falling back to default fonts", resourcePath);
            return;
        }

        try (InputStream ignored = resource.openStream()) {
            builder.useFont(
                () -> openFontStream(resource, resourcePath),
                familyName,
                weight,
                BaseRendererBuilder.FontStyle.NORMAL,
                true
            );
        } catch (IOException ex) {
            log.warn("Failed to load font {}: {}", resourcePath, ex.getMessage());
        }
    }

    private InputStream openFontStream(URL resource, String resourcePath) {
        try {
            return resource.openStream();
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to open font stream for " + resourcePath, ex);
        }
    }

    private String formatCurrency(long amountMinorUnits, Currency currency) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.ENGLISH);
        format.setCurrency(currency);
        return format.format(amountMinorUnits / 100.0);
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
}
