package net.theflapjack.fa_report.model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlatTransaction {
    private String portfolioShortName;
    private String securityName;
    private String securityISIN;
    private String currencyCode;
    private Double quantity; //Would normally set to int but some exchanges allow for fractional quantities (common for FX)
    private Double unitPrice;
    private Double tradeAmount; //Executed quantity?
    private String typeName;
    private String transactionDate;
    private String settlementDate;
    private Double notionalPriceUSD;
    private Double targetFXValue;
    private Double notionalPriceTarget;
}
