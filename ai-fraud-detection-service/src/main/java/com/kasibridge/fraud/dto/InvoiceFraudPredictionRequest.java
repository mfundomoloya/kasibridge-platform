package com.kasibridge.fraud.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceFraudPredictionRequest {

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal invoiceAmount;

    @NotNull
    @Min(0)
    @Max(23)
    private Integer submissionHour;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal supplierInvoiceCount30d;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal supplierAvgAmount90d;

    @NotNull
    private BigDecimal invoiceAmountZscore;

    @NotNull
    @Min(0)
    private Integer supplierAgeDays;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal avgInvoiceAmount;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal annualBudget;

    @DecimalMin("0.0")
    private BigDecimal ocrTotalExtracted;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer invoiceMonth;

    @NotNull
    @Min(0)
    @Max(6)
    private Integer invoiceDayOfWeek;

    @NotNull
    @Min(1)
    @Max(31)
    private Integer invoiceDayOfMonth;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer invoiceIsWeekend;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer hasInvoiceImage;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer hasOcrResult;

    private BigDecimal ocrAmountDifference;

    @DecimalMin("0.0")
    private BigDecimal ocrAmountDifferenceAbs;

    @DecimalMin("0.0")
    private BigDecimal ocrAmountDifferenceRatio;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer duplicateInvoiceFlag;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer splitInvoiceFlag;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer lateNightSubmissionFlag;

    @NotNull
    @DecimalMin("0.0")
    @DecimalMax("1.0")
    private BigDecimal supplierRiskScore;

    @NotNull
    @Min(0)
    @Max(1)
    private Integer blacklistedFlag;

    /*
     * Null means that no image-tamper
     * assessment was available.
     */
    @Min(0)
    @Max(1)
    private Integer imageTamperFlag;

    @NotBlank
    private String paymentTerms;

    @NotBlank
    private String invoiceType;

    @NotBlank
    @Size(min = 2, max = 3)
    private String supplierCountry;

    @NotBlank
    private String region;
}