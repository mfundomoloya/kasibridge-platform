package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.WhatsAppTemplateContext;
import com.kasibridge.procurement.entity.NotificationOutbox;

public interface WhatsAppMessageTemplateService {
    String generateMessage(NotificationOutbox.NotificationTemplateType templateType, WhatsAppTemplateContext context);
}
