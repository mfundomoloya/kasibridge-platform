package com.kasibridge.procurement.service;

import com.fasterxml.jackson.databind.JsonNode;

public interface WhatsAppWebhookService {

    void processWebhook(JsonNode payload);
}
