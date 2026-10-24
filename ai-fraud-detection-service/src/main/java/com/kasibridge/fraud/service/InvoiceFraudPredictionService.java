package com.kasibridge.fraud.service;

import com.kasibridge.fraud.dto.InvoiceFraudPredictionRequest;
import com.kasibridge.fraud.dto.InvoiceFraudPredictionResponse;
import com.kasibridge.fraud.dto.MlInferenceRequest;
import com.kasibridge.fraud.dto.MlInferenceResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceFraudPredictionService {

    private final MlInferenceClient
            mlInferenceClient;

    public InvoiceFraudPredictionResponse predict(
            InvoiceFraudPredictionRequest request
    ) {
        MlInferenceRequest inferenceRequest =
                MlInferenceRequest.from(
                        request
                );

        MlInferenceResponse inferenceResponse =
                mlInferenceClient.predict(
                        inferenceRequest
                );

        InvoiceFraudPredictionResponse response =
                InvoiceFraudPredictionResponse.from(
                        inferenceResponse
                );

        log.info(
                "Procurement invoice prediction "
                        + "completed: classification={} "
                        + "riskLevel={} modelVersion={}",
                response.classification(),
                response.riskLevel(),
                response.modelVersion()
        );

        return response;
    }
}