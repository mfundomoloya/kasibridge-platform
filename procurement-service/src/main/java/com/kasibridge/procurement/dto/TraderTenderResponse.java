package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.Tender;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TraderTenderResponse {

    private Long id;
    private String tenderReference;
    private String title;
    private String description;
    private String evaluationCriteria;
    private BigDecimal budgetAmount;
    private String buyerOrgId;
    private Tender.TenderStatus status;
    private LocalDateTime publishedAt;

    public static TraderTenderResponse from(
            Tender tender
    ) {
        return TraderTenderResponse.builder()
                .id(tender.getId())
                .tenderReference(
                        tender.getTenderReference()
                )
                .title(tender.getTitle())
                .description(tender.getDescription())
                .evaluationCriteria(
                        tender.getEvaluationCriteria()
                )
                .budgetAmount(
                        tender.getBudgetAmount()
                )
                .buyerOrgId(
                        tender.getBuyerOrgId()
                )
                .status(tender.getStatus())
                .publishedAt(
                        tender.getPublishedAt()
                )
                .build();
    }
}