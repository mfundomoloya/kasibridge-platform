package com.kasibridge.procurement.dto;

import com.kasibridge.procurement.entity.BidComplianceResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class BidComplianceResponse {

    private Long id;
    private Long bidId;
    private boolean csdValid;
    private boolean taxClearanceValid;
    private boolean bbbeeValid;
    private boolean requiredDocumentsUploaded;
    private boolean passed;
    private String failureReason;
    private LocalDateTime checkedAt;

    public static BidComplianceResponse from(BidComplianceResult result) {

        return BidComplianceResponse.builder()
                .id(result.getId())
                .bidId(result.getBidId())
                .csdValid(result.isCsdValid())
                .taxClearanceValid(result.isTaxClearanceValid())
                .bbbeeValid(result.isBbbeeValid())
                .requiredDocumentsUploaded(result.isRequiredDocumentsUploaded())
                .passed(result.isPassed())
                .failureReason(result.getFailureReason())
                .checkedAt(result.getCheckedAt())
                .build();
    }
}
