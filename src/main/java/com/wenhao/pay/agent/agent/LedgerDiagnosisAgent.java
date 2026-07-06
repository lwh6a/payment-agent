package com.wenhao.pay.agent.agent;

import com.wenhao.pay.agent.model.enums.IssueType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/** 分账排障 Agent。 */
@Component
public class LedgerDiagnosisAgent extends AbstractDiagnosisAgent {

    public LedgerDiagnosisAgent(@Qualifier("ledgerAgentClient") ChatClient chatClient,
                                MessageChatMemoryAdvisor memoryAdvisor) {
        super(chatClient, memoryAdvisor);
    }

    @Override
    public IssueType supports() {
        return IssueType.LEDGER;
    }
}
