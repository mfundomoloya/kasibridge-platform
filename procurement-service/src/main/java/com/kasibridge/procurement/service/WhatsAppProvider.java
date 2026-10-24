package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.WhatsAppDeliveryCommand;
import com.kasibridge.procurement.dto.WhatsAppDeliveryResult;

public interface WhatsAppProvider {

    WhatsAppDeliveryResult sendMessage(WhatsAppDeliveryCommand command);
}