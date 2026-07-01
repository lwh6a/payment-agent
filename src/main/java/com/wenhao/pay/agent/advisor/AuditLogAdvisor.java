package com.wenhao.pay.agent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

/**
 * 审计日志 Advisor：记录每次对话的输入问题与模型最终回答，用于排障留痕与回溯。
 * （单次 Tool 调用的细粒度日志见 {@code ToolCallLogAspect}）
 */
@Component
public class AuditLogAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAdvisor.class);

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        log.info("[审计-提问] {}", userText(request));
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        log.info("[审计-回答] {}", answerText(response));
        return response;
    }

    private String userText(ChatClientRequest request) {
        var userMessage = request.prompt().getUserMessage();
        return userMessage == null ? "" : userMessage.getText();
    }

    private String answerText(ChatClientResponse response) {
        if (response.chatResponse() == null || response.chatResponse().getResult() == null) {
            return "";
        }
        return response.chatResponse().getResult().getOutput().getText();
    }

    @Override
    public String getName() {
        return "AuditLogAdvisor";
    }

    @Override
    public int getOrder() {
        return 1000; // 最外层，包住整条链
    }
}
