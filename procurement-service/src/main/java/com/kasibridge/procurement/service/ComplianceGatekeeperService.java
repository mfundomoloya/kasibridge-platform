package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.SubmitBidRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ComplianceGatekeeperService {

    public ComplianceDecision evaluate(SubmitBidRequest request){
        List<String> failures = new ArrayList<>();

        if(!request.isCsdValid()){
            failures.add("CSD registration is invalid or missing");
        }

        if(!request.isTaxClearanceValid()){
            failures.add("Tax clearance is invalid or missing");
        }

        if(!request.isBbbeeValid()){
            failures.add("B-BBEE is invalid or missing");
        }

        if(!request.isRequiredDocumentsUploaded()){
            failures.add("Required documents were not uploaded");
        }

        boolean passed = failures.isEmpty();

        return new ComplianceDecision(
                passed,
                passed ? null : String.join("; ", failures)
        );
    }

    public record ComplianceDecision(
            boolean passed,
            String failureReason
    ){
    }
}
