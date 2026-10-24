package com.kasibridge.fraud.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MlInferenceRequest(

        @JsonProperty("invoice_amount")
        BigDecimal invoiceAmount,

        @JsonProperty("submission_hour")
        Integer submissionHour,

        @JsonProperty("supplier_invoice_count_30d")
        BigDecimal supplierInvoiceCount30d,

        @JsonProperty("supplier_avg_amount_90d")
        BigDecimal supplierAvgAmount90d,

        @JsonProperty("invoice_amount_zscore")
        BigDecimal invoiceAmountZscore,

        @JsonProperty("supplier_age_days")
        Integer supplierAgeDays,

        @JsonProperty("avg_invoice_amount")
        BigDecimal avgInvoiceAmount,

        @JsonProperty("annual_budget")
        BigDecimal annualBudget,

        @JsonProperty("ocr_total_extracted")
        BigDecimal ocrTotalExtracted,

        @JsonProperty("invoice_month")
        Integer invoiceMonth,

        @JsonProperty("invoice_day_of_week")
        Integer invoiceDayOfWeek,

        @JsonProperty("invoice_day_of_month")
        Integer invoiceDayOfMonth,

        @JsonProperty("invoice_is_weekend")
        Integer invoiceIsWeekend,

        @JsonProperty("has_invoice_image")
        Integer hasInvoiceImage,

        @JsonProperty("has_ocr_result")
        Integer hasOcrResult,

        @JsonProperty("ocr_amount_difference")
        BigDecimal ocrAmountDifference,

        @JsonProperty("ocr_amount_difference_abs")
        BigDecimal ocrAmountDifferenceAbs,

        @JsonProperty("ocr_amount_difference_ratio")
        BigDecimal ocrAmountDifferenceRatio,

        @JsonProperty("duplicate_invoice_flag")
        Integer duplicateInvoiceFlag,

        @JsonProperty("split_invoice_flag")
        Integer splitInvoiceFlag,

        @JsonProperty("late_night_submission_flag")
        Integer lateNightSubmissionFlag,

        @JsonProperty("supplier_risk_score")
        BigDecimal supplierRiskScore,

        @JsonProperty("blacklisted_flag")
        Integer blacklistedFlag,

        @JsonProperty("image_tamper_flag")
        Integer imageTamperFlag,

        @JsonProperty("payment_terms")
        String paymentTerms,

        @JsonProperty("invoice_type")
        String invoiceType,

        @JsonProperty("supplier_country")
        String supplierCountry,

        @JsonProperty("region")
        String region
) {

    public static MlInferenceRequest from(
            InvoiceFraudPredictionRequest request
    ) {
        return new MlInferenceRequest(
                request.getInvoiceAmount(),
                request.getSubmissionHour(),
                request.getSupplierInvoiceCount30d(),
                request.getSupplierAvgAmount90d(),
                request.getInvoiceAmountZscore(),
                request.getSupplierAgeDays(),
                request.getAvgInvoiceAmount(),
                request.getAnnualBudget(),
                request.getOcrTotalExtracted(),
                request.getInvoiceMonth(),
                request.getInvoiceDayOfWeek(),
                request.getInvoiceDayOfMonth(),
                request.getInvoiceIsWeekend(),
                request.getHasInvoiceImage(),
                request.getHasOcrResult(),
                request.getOcrAmountDifference(),
                request.getOcrAmountDifferenceAbs(),
                request.getOcrAmountDifferenceRatio(),
                request.getDuplicateInvoiceFlag(),
                request.getSplitInvoiceFlag(),
                request.getLateNightSubmissionFlag(),
                request.getSupplierRiskScore(),
                request.getBlacklistedFlag(),
                request.getImageTamperFlag(),
                request.getPaymentTerms().trim(),
                request.getInvoiceType().trim(),
                request.getSupplierCountry()
                        .trim()
                        .toUpperCase(),
                request.getRegion().trim()
        );
    }
}