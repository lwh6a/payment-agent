package com.wenhao.pay.agent.advisor;

import com.wenhao.pay.agent.prompt.SystemPrompts;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 上下文增强 Advisor：在请求送入模型前，追加一条领域元数据系统消息
 * （状态机枚举、支付方式段、关键 Topic、金额单位等），降低模型幻觉、统一口径。
 */
@Component
public class ContextEnrichAdvisor implements BaseAdvisor {

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        Prompt prompt = request.prompt();
        List<Message> messages = new ArrayList<>(prompt.getInstructions());
        messages.add(new SystemMessage(SystemPrompts.DOMAIN_CONTEXT));
        Prompt enriched = new Prompt(messages, prompt.getOptions());
        return request.mutate().prompt(enriched).build();
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "ContextEnrichAdvisor";
    }

    @Override
    public int getOrder() {
        return 100; // 在安全护栏之后、审计之前
    }
}
