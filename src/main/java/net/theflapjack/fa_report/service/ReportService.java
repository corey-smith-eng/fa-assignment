package net.theflapjack.fa_report.service;

import net.theflapjack.fa_report.auth.TokenManager;
import org.springframework.stereotype.Service;
import net.theflapjack.fa_report.client.FaGraphQLClient;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.theflapjack.fa_report.model.FlatTransaction;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;




@Service
public class ReportService {

    private final FaGraphQLClient graphQLClient;
    private final TokenManager tokenManager;
    private final String username;
    private final String password;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DecimalFormat MONEY = new DecimalFormat("0.00");
    private static final DecimalFormat HUMAN_MONEY = new DecimalFormat("$#,##0.00");

    public ReportService(FaGraphQLClient graphQLClient,
                         TokenManager tokenManager,
                         @Value("${fa.api.username}") String username,
                         @Value("${fa.api.password}") String password) {
        this.graphQLClient = graphQLClient;
        this.tokenManager = tokenManager;
        this.username = username;
        this.password = password;
    }

    public List<FlatTransaction> getTransactions(Long portfolioId, String startDate, String endDate, String targetCurrency) {

        String startDateString = (startDate == null) ? "" : startDate;
        String endDateString = (endDate == null) ? "" : endDate;

        String transactionQuery = """
            query Transactions($ids: [Long], $startDate: String, $endDate: String, $targetCurrency: String) {
              portfoliosByIds(ids: $ids) {
                transactions(status: "OK", startDate: $startDate, endDate: $endDate) {
                  portfolio: parentPortfolio {
                    shortName
                  }
                  security {
                    name
                    isinCode
                  }
                  currency {
                    code: securityCode
                  }
                  quantity: amount
                  unitPrice: unitPriceView
                  tradeAmount
                  type {
                    name: typeName
                  }
                  transactionDate
                  settlementDate
                  fxUSD: fxRate(quoteCurrency: "USD")
                  fxTarget: fxRate(quoteCurrency: $targetCurrency)
                }
              }
            }
        """;

        String transactionData = String.format("""
            {
                "ids": [%d],
                "startDate": "%s",
                "endDate": "%s",
                "targetCurrency": "%s"
            }
        """, portfolioId, startDateString, endDateString,targetCurrency);

        String token = tokenManager.getValidAccessToken(username,password);
        String response = graphQLClient.sendQuery(transactionQuery,transactionData,token);

        return parseFlatTransactions(response);
    }


    // Wrapper to output a csv string (separated logic so formatted transactions could still be used with other code)
    public String generateCsv(Long portfolioId, String startDate, String endDate, String customFX) {
        List<FlatTransaction> transactions = getTransactions(portfolioId, startDate, endDate, customFX);
        return convertToCsv(transactions);
    }

    public String generateHumanCsv(Long portfolioId, String startDate, String endDate, String customFX) {
        List<FlatTransaction> transactions = getTransactions(portfolioId, startDate, endDate, customFX);
        return convertToHumanCsv(transactions);
    }


    private List<FlatTransaction> parseFlatTransactions(String json) {
        List<FlatTransaction> result = new ArrayList<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(json);
            JsonNode transactions = root
                    .path("data")
                    .path("portfoliosByIds")
                    .get(0)
                    .path("transactions");

            for (JsonNode tx : transactions) {

                Double quantity = tx.path("quantity").asDouble();
                Double unitPrice = tx.path("unitPrice").asDouble();
                Double tradeAmount = tx.path("tradeAmount").asDouble();
                Double fxUSD = tx.path("fxUSD").asDouble();
                Double fxTarget = tx.path("fxTarget").asDouble();

                        FlatTransaction flat = new FlatTransaction(
                        safeText(tx.path("portfolio").path("shortName")),
                        safeText(tx.path("security").path("name")),
                        safeText(tx.path("security").path("isinCode")),
                        safeText(tx.path("currency").path("code")),
                        quantity,
                        unitPrice,
                        tradeAmount,
                        safeText(tx.path("type").path("name")),
                        safeText(tx.path("transactionDate")),
                        safeText(tx.path("settlementDate")),
                        unitPrice*fxUSD*tradeAmount,
                                fxTarget,
                                unitPrice*fxTarget*tradeAmount
                );
                result.add(flat);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse transactions", e);
        }
        return result;
    }

    // Handles none/null/missing values to "" for consistency
    private String safeText(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? "" : node.asText();
    }


    // Specifically to hit the conditions of the project scope
    private String convertToCsv(List<FlatTransaction> transactions) {
        StringBuilder csv = new StringBuilder();

        csv.append("portfolio,security,isin,currency,quantity,unit_price,trade_amount,type,trade_date,settlement_date\n");

        for (FlatTransaction tx : transactions) {
            csv.append(String.format(
                    "%s,%s,%s,%s,%.2f,%.2f,%.2f,%s,%s,%s\n",
                    safe(tx.getPortfolioShortName()),
                    safe(tx.getSecurityName()),
                    safe(tx.getSecurityISIN()),
                    safe(tx.getCurrencyCode()),
                    tx.getQuantity() != null ? tx.getQuantity() : 0.0,
                    tx.getUnitPrice() != null ? tx.getUnitPrice() : 0.0,
                    tx.getTradeAmount() != null ? tx.getTradeAmount() : 0.0,
                    safe(tx.getTypeName()),
                    safe(tx.getTransactionDate()),
                    safe(tx.getSettlementDate())
            ));
        }

        return csv.toString();
    }

    // CSV more so for humans to read in a financial report way
    private String convertToHumanCsv(List<FlatTransaction> transactions) {
        StringBuilder csv = new StringBuilder("\uFEFF");

        csv.append("Portfolio Short Name,Security Name,ISIN,Currency Code,Quantity,Unit Price,Trade Amount,Notional (USD), Target FX Rate, Notional (Target), Type Name,Transaction Date,Settlement Date\n");

        transactions.sort(Comparator
                .comparing(FlatTransaction::getTypeName, Comparator.nullsLast(String::compareToIgnoreCase))
                .thenComparing(FlatTransaction::getTransactionDate, Comparator.nullsLast(String::compareTo))
        );

        for (FlatTransaction tx : transactions) {
            boolean skipAmountFields = tx.getTypeName() != null &&
                    (tx.getTypeName().equalsIgnoreCase("Split") ||
                            tx.getTypeName().equalsIgnoreCase("Add Contract"));

            String quantity = skipAmountFields ? "" : formatDouble(tx.getQuantity());
            String unitPrice = skipAmountFields ? "" : formatDouble(tx.getUnitPrice());
            String tradeAmount = skipAmountFields ? "" : formatDouble(tx.getTradeAmount());
            String fxTarget = skipAmountFields ? "" : formatDouble(tx.getTargetFXValue());
            String USDNotionalPrice = skipAmountFields ? "" : formatDouble(tx.getNotionalPriceUSD());
            String targetNotionalPrice = skipAmountFields ? "" : formatDouble(tx.getNotionalPriceTarget());



            csv.append(String.format(
                    "%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                    safe(tx.getPortfolioShortName()),
                    safe(tx.getSecurityName()),
                    safe(tx.getSecurityISIN()),
                    safe(tx.getCurrencyCode()),
                    quantity,
                    unitPrice,
                    tradeAmount,
                    USDNotionalPrice,
                    fxTarget,
                    targetNotionalPrice,
                    safe(tx.getTypeName()),
                    formatDate(tx.getTransactionDate()),
                    formatDate(tx.getSettlementDate())
            ));
        }
        double[] summary = summarizeCashFlows(transactions);
        csv.append("\nSummary (USD),,,\n");
        csv.append(String.format("Total Cash In:,,,\"%s\"\n", HUMAN_MONEY.format(summary[0])));
        csv.append(String.format("Total Cash Out:,,,\"%s\"\n", HUMAN_MONEY.format(summary[1])));
        csv.append(String.format("Net Flow:,,,\"%s\"\n", HUMAN_MONEY.format(summary[0] - summary[1])));

        return csv.toString();
    }


    private String safe(String s) {
        return s == null ? "" : s.replace(",", "");
    }

    private String formatDouble(Double d) {
        return d == null ? "" : MONEY.format(d);
    }

    private String formatDate(String dateStr) {
        try {
            LocalDate date = LocalDate.parse(dateStr);
            return date.format(DATE_FORMAT);
        } catch (Exception e) {
            return "";
        }
    }


    private double[] summarizeCashFlows(List<FlatTransaction> transactions) {
        double in = 0.0;
        double out = 0.0;

        for (FlatTransaction tx : transactions) {
            String type = tx.getTypeName() == null ? "" : tx.getTypeName().toLowerCase();
            double amount = tx.getNotionalPriceUSD() != null ? tx.getNotionalPriceUSD() : 0.0;

            if (List.of(
                    "cashflow in", "deposit", "cashflow in (internal)",
                    "sell", "redemption", "expire").contains(type)) {
                in += amount;
            } else if (List.of(
                    "buy", "cashflow out", "cashflow out (internal)",
                    "management fee", "subscription", "exercise subscription right (c)").contains(type)) {
                out += amount;
            }
        }
        return new double[]{in, out};
    }



}
