package com.wenhao.pay.agent.agent;

import com.wenhao.pay.agent.model.dto.IssueClassification;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

/**
 * 领域 Agent 公共逻辑：统一拼装用户提示词、统一调用各自的 ChatClient。
 * 子类只需提供 supports() 和对应的 ChatClient。
 */
public abstract class AbstractDiagnosisAgent implements DiagnosisAgent {

    protected final ChatClient chatClient;

    protected AbstractDiagnosisAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String diagnose(String question, IssueClassification classification) {
        return chatClient.prompt()
                .user(buildUserPrompt(question, classification))
                .call()
                .content();
    }

    @Override
    public Flux<String> diagnoseStream(String question, IssueClassification classification) {
        return chatClient.prompt()
                .user(buildUserPrompt(question, classification))
                .stream()
                .content();
    }

    /** 把编排层抽取出的参数一并交给领域 Agent，减少其重复抽参。 */
    protected String buildUserPrompt(String question, IssueClassification c) {
        return """
                用户问题：%s
                已抽取参数：tradeNo=%s, orderNo=%s, merchantCode=%s
                请按你的排障决策链调用工具完成诊断，并输出结构化排障报告。
                """.formatted(question, c.tradeNo(), c.orderNo(), c.merchantCode());
    }
}
