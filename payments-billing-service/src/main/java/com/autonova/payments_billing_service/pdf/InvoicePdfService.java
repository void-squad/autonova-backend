package com.autonova.payments_billing_service.pdf;

import com.autonova.payments_billing_service.domain.InvoiceEntity;
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class InvoicePdfService {

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

        String amountFormatted = formatCurrency(invoice.getAmountTotal(), invoice.getCurrency());
        context.setVariable("amountDueFormatted", amountFormatted);
        context.setVariable("currencyCode", invoice.getCurrency());

        String html = templateEngine.process("invoice", context);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(
                () -> InvoicePdfService.class.getResourceAsStream("/fonts/Lato-Regular.ttf"),
                "Lato",
                400,
                BaseRendererBuilder.FontStyle.NORMAL,
                true
            );
            builder.useFont(
                () -> InvoicePdfService.class.getResourceAsStream("/fonts/Lato-Bold.ttf"),
                "Lato",
                700,
                BaseRendererBuilder.FontStyle.NORMAL,
                true
            );
            builder.withHtmlContent(html, null);
            builder.toStream(outputStream);
            builder.run();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to generate invoice PDF", e);
        }
    }

    private String formatCurrency(long amountMinorUnits, String currencyCode) {
        NumberFormat format = NumberFormat.getCurrencyInstance(Locale.ENGLISH);
        format.setCurrency(Currency.getInstance(currencyCode));
        return format.format(amountMinorUnits / 100.0);
    }

    private String formatDate(OffsetDateTime timestamp) {
        if (timestamp == null) {
            return "—";
        }
        return timestamp.atZoneSameInstant(INVOICE_ZONE).format(INVOICE_DATE_FORMATTER);
    }
}
