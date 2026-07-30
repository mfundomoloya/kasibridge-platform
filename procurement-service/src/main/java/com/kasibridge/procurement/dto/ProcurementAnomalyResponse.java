package com.kasibridge.procurement.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProcurementAnomalyResponse {

    private Long tenderId;
    private Long bidId;
    private String type;
    private String severity;
    private String message;
    private String evidence;
}
