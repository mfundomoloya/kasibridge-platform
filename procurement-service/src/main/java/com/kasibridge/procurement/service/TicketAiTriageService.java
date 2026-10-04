package com.kasibridge.procurement.service;

import com.kasibridge.procurement.dto.TicketAiTriageResult;
import com.kasibridge.procurement.entity.SupportTicket;

public interface TicketAiTriageService {

    TicketAiTriageResult assessTicket(SupportTicket ticket);
}
