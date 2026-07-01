package com.wenhao.pay.agent.agent;

import com.wenhao.pay.agent.model.enums.IssueType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** 支付排障 Agent。 */
@Component
public class PaymentDiagnosisAgent extends AbstractDiagnosisAgent {

    public PaymentDiagnosisAgent(@Qualifier("paymentAgentClient") ChatClient chatClient) {
        super(chatClient);
    }

    @Override
    public IssueType supports() {
        return IssueType.PAYMENT;
    }
}
