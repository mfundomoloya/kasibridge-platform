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

    /*
     * These business identifiers are optional so
     * existing /invoices/predict clients continue
     * to work.
     *
     * The persistence service will generate an
     * invoice reference when it is absent.
     */
    @Size(max = 100, message = "Invoice reference cannot exceed 100 characters.")
    private String invoiceReference;

    @Size(max = 100, message = "Supplier ID cannot exceed 100 characters.")
    private String supplierId;

    @Size(max = 100, message = "Department ID cannot exceed 100 characters.")
    private String departmentId;

    @NotNull(message = "Invoice amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Invoice amount cannot be negative.")
    private BigDecimal invoiceAmount;

    @NotNull(message = "Submission hour is required.")
    @Min(value = 0, message = "Submission hour cannot be less than 0.")
    @Max(value = 23, message = "Submission hour cannot be greater than 23.")
    private Integer submissionHour;

    @NotNull(message = "Supplier invoice count is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Supplier invoice count cannot be negative.")
    private BigDecimal supplierInvoiceCount30d;

    @NotNull(message = "Supplier average amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Supplier average amount cannot be negative.")
    private BigDecimal supplierAvgAmount90d;

    @NotNull(message ="Invoice amount z-score is required.")
    private BigDecimal invoiceAmountZscore;

    @NotNull(message = "Supplier age is required.")
    @Min(value = 0, message = "Supplier age cannot be negative.")
    private Integer supplierAgeDays;

    @NotNull(message = "Average invoice amount is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Average invoice amount cannot be negative.")
    private BigDecimal avgInvoiceAmount;

    @NotNull(message = "Annual budget is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Annual budget cannot be negative.")
    private BigDecimal annualBudget;

    /*
     * OCR fields may be null when the invoice
     * did not include an image or OCR extraction
     * was unavailable.
     */
    @DecimalMin(value = "0.0", inclusive = true, message = "OCR total cannot be negative.")
    private BigDecimal ocrTotalExtracted;

    @NotNull(message = "Invoice month is required.")
    @Min(value = 1, message = "Invoice month cannot be less than 1.")
    @Max(value = 12, message = "Invoice month cannot be greater than 12.")
    private Integer invoiceMonth;

    @NotNull(message = "Invoice day of week is required.")
    @Min(value = 0, message = "Invoice day of week cannot be less than 0.")
    @Max(value = 6, message = "Invoice day of week cannot be greater than 6.")
    private Integer invoiceDayOfWeek;

    @NotNull(message = "Invoice day of month is required.")
    @Min(value = 1, message = "Invoice day of month cannot be less than 1.")
    @Max(value = 31, message = "Invoice day of month cannot be greater than 31.")
    private Integer invoiceDayOfMonth;

    @NotNull(message = "Invoice weekend flag is required.")
    @Min(value = 0, message = "Invoice weekend flag must be 0 or 1.")
    @Max(value = 1, message = "Invoice weekend flag must be 0 or 1.")
    private Integer invoiceIsWeekend;

    @NotNull(message = "Invoice image flag is required.")
    @Min(value = 0, message = "Invoice image flag must be 0 or 1.")
    @Max(value = 1, message = "Invoice image flag must be 0 or 1.")
    private Integer hasInvoiceImage;

    @NotNull(message = "OCR result flag is required.")
    @Min(value = 0, message = "OCR result flag must be 0 or 1.")
    @Max(value = 1, message = "OCR result flag must be 0 or 1.")
    private Integer hasOcrResult;

    private BigDecimal ocrAmountDifference;

    @DecimalMin(value = "0.0", inclusive = true, message = "Absolute OCR difference cannot be negative.")
    private BigDecimal
            ocrAmountDifferenceAbs;

    @DecimalMin(value = "0.0", inclusive = true, message = "OCR difference ratio cannot be negative.")
    private BigDecimal ocrAmountDifferenceRatio;

    @NotNull(message = "Duplicate invoice flag is required.")
    @Min(value = 0, message = "Duplicate invoice flag must be 0 or 1.")
    @Max(value = 1, message = "Duplicate invoice flag must be 0 or 1.")
    private Integer duplicateInvoiceFlag;

    @NotNull(message = "Split invoice flag is required.")
    @Min(value = 0, message = "Split invoice flag must be 0 or 1.")
    @Max(value = 1, message = "Split invoice flag must be 0 or 1.")
    private Integer splitInvoiceFlag;

    @NotNull(message = "Late-night submission flag is required.")
    @Min(value = 0, message = "Late-night submission flag must be 0 or 1.")
    @Max(value = 1, message = "Late-night submission flag must be 0 or 1.")
    private Integer lateNightSubmissionFlag;

    @NotNull(message = "Supplier risk score is required.")
    @DecimalMin(value = "0.0", inclusive = true, message = "Supplier risk score cannot be less than 0.")
    @DecimalMax(value = "1.0", inclusive = true, message = "Supplier risk score cannot be greater than 1.")
    private BigDecimal supplierRiskScore;

    @NotNull(message = "Blacklisted flag is required.")
    @Min(value = 0, message = "Blacklisted flag must be 0 or 1.")
    @Max(value = 1, message = "Blacklisted flag must be 0 or 1.")
    private Integer blacklistedFlag;

    /*
     * Null means that no image-tamper
     * assessment was available.
     *
     * Zero means that an image was assessed
     * and no tampering was detected.
     */
    @Min(value = 0, message = "Image tamper flag must be 0 or 1.")
    @Max(value = 1, message = "Image tamper flag must be 0 or 1.")
    private Integer imageTamperFlag;

    @NotBlank(message = "Payment terms are required.")
    @Size(max = 30, message = "Payment terms cannot exceed " + "30 characters.")
    private String paymentTerms;

    @NotBlank(message = "Invoice type is required.")
    @Size(max = 30, message = "Invoice type cannot exceed " + "30 characters.")
    private String invoiceType;

    @NotBlank(message = "Supplier country is required.")
    @Size(min = 2, max = 3, message = "Supplier country must contain " + "2 or 3 characters.")
    private String supplierCountry;

    @NotBlank(message = "Region is required.")
    @Size(max = 100, message = "Region cannot exceed " + "100 characters.")
    private String region;
}