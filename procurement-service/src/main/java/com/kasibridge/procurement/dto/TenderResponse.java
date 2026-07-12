package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Tender;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TenderResponse {

    private Long id;
    private String tenderReference;
    private String title;
    private String description;
    private String evaluationCriteria;
    private BigDecimal budgetAmount;
    private String buyerOrgId;
    private Long createdByUserId;
    private Tender.TenderStatus status;
    private String specificationHash;
    private LocalDateTime publishedAt;
    private LocalDateTime closedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TenderResponse from(Tender tender) {
        return TenderResponse.builder()
                .id(tender.getId())
                .tenderReference(tender.getTenderReference())
                .title(tender.getTitle())
                .description(tender.getDescription())
                .evaluationCriteria(tender.getEvaluationCriteria())
                .budgetAmount(tender.getBudgetAmount())
                .buyerOrgId(tender.getBuyerOrgId())
                .createdByUserId(tender.getCreatedByUserId())
                .status(tender.getStatus())
                .specificationHash(tender.getSpecificationHash())
                .publishedAt(tender.getPublishedAt())
                .closedAt(tender.getClosedAt())
                .createdAt(tender.getCreatedAt())
                .updatedAt(tender.getUpdatedAt())
                .build();
    }
}
