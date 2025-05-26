package net.theflapjack.fa_report.controller;

import net.theflapjack.fa_report.client.FaGraphQLClient;
import net.theflapjack.fa_report.service.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/report")
public class ReportController {

    private final ReportService reportService;
    private static final Logger logger = LoggerFactory.getLogger(ReportController.class);

    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_DATE;

    private static final Set<String> VALID_CURRENCIES = Set.of(
            "USD", "EUR", "GBP", "JPY", "AUD", "CAD", "CHF", "CNY", "SEK", "NZD"
    );

    public ReportController(ReportService reportService){
        this.reportService = reportService;
    }

    @GetMapping
    public ResponseEntity<String> getReport(
            @RequestParam(required = true) Long portfolioId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "false") boolean pretty,
            @RequestParam(defaultValue = "USD") String targetCurrency
    ) {
        try {
            // Check dates if provided
            LocalDate start = null;
            LocalDate end = null;

            if (startDate != null) {
                start = LocalDate.parse(startDate, ISO_DATE);
            }
            if (endDate != null) {
                end = LocalDate.parse(endDate, ISO_DATE);
            }

            if (start != null && end != null && end.isBefore(start)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("endDate must not be before startDate.");
            }

            // Check target currency (I haven't verified each currency in the list works just the first few)
            if (!VALID_CURRENCIES.contains(targetCurrency.toUpperCase())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Invalid targetCurrency. Accepted values are: " + VALID_CURRENCIES);
            }

            String transactionCSV = pretty
                    ? reportService.generateHumanCsv(portfolioId, startDate, endDate, targetCurrency)
                    : reportService.generateCsv(portfolioId, startDate, endDate, targetCurrency);

            String csvName = String.format("portfolio_%d_%s.csv", portfolioId, pretty ? "summary" : "raw");

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + csvName)
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .body(transactionCSV);

        } catch (DateTimeParseException e) {
            logger.warn("Invalid date format", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Dates must be in ISO format (YYYY-MM-DD).");
        } catch (Exception e) {
            logger.error("Failed to generate report", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error generating report");
        }
    }
}
