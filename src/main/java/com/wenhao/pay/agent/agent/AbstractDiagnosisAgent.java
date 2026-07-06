package com.wenhao.pay.agent.agent;

import com.wenhao.pay.agent.model.dto.IssueClassification;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.util.Objects;

/**
 * 领域 Agent 公共逻辑：统一拼装用户提示词、统一调用各自的 ChatClient。
 * 子类只需提供 supports() 和对应的 ChatClient。
 *
 * 带 sessionId 的请求会挂载会话记忆 Advisor，实现多轮追问；不带 sessionId 则单轮无状态。
 */
public abstract class AbstractDiagnosisAgent implements DiagnosisAgent {

    protected final ChatClient chatClient;
    protected final MessageChatMemoryAdvisor memoryAdvisor;

    protected AbstractDiagnosisAgent(ChatClient chatClient, MessageChatMemoryAdvisor memoryAdvisor) {
        this.chatClient = chatClient;
        this.memoryAdvisor = memoryAdvisor;
    }

    @Override
    public String diagnose(String question, IssueClassification classification, String sessionId) {
        return buildRequest(question, classification, sessionId)
                .call()
                .content();
    }

    @Override
    public Flux<String> diagnoseStream(String question, IssueClassification classification, String sessionId) {
        return buildRequest(question, classification, sessionId)
                .stream()
                .content();
    }

    private ChatClient.ChatClientRequestSpec buildRequest(String question, IssueClassification c, String sessionId) {
        ChatClient.ChatClientRequestSpec spec = chatClient.prompt()
                .user(buildUserPrompt(question, c));
        if (StringUtils.hasText(sessionId)) {
            spec = spec.advisors(memoryAdvisor)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId));
        }
        return spec;
    }

    /** 把编排层抽取出的参数一并交给领域 Agent，减少其重复抽参。 */
    protected String buildUserPrompt(String question, IssueClassification c) {
        return """
                用户问题：%s
                已抽取参数：tradeNo=%s, orderNo=%s, merchantCode=%s
                请按你的排障决策链调用工具完成诊断，并输出结构化排障报告。
                """.formatted(question,
                Objects.requireNonNullElse(c.tradeNo(), "未提供"),
                Objects.requireNonNullElse(c.orderNo(), "未提供"),
                Objects.requireNonNullElse(c.merchantCode(), "未提供"));
    }
}
