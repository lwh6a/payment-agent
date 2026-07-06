package com.wenhao.pay.agent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全护栏 Advisor：在请求进入模型前拦截提示注入。
 *
 * 排障系统的安全是三层防线：
 * 1. Tool 层只读 + 表白名单（ToolQueryGuard）——硬边界，模型没有任何写能力；
 * 2. 编排意图识别输出 unsafe 字段——由模型语义判定写操作/转账等危险请求（见 OrchestratorAgent）；
 * 3. 本 Advisor——只拦截明显的提示注入特征词。
 *
 * 不再用"转账""删除"等业务词做黑名单：这些词在正常排障提问里大量出现（如"用户转账没到账"），
 * 关键词匹配误伤率高，危险意图交给第 2 层语义判定。
 */
@Component
public class SafetyGuardAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SafetyGuardAdvisor.class);

    /** 提示注入特征关键词（命中即拦截）。 */
    private static final List<String> BLOCK_WORDS = List.of(
            "忽略以上", "忽略之前", "忽略上面", "ignore previous", "ignore all previous",
            "system prompt", "系统提示词", "你现在是", "假装你是"
    );

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        var userMessage = request.prompt().getUserMessage();
        String text = userMessage == null ? "" : userMessage.getText().toLowerCase();
        for (String word : BLOCK_WORDS) {
            if (text.contains(word)) {
                log.warn("[安全护栏-拦截] 命中提示注入特征={} 原文={}", word, text);
                throw new UnsafeRequestException("请求被安全护栏拦截：请直接描述排障问题，不要包含指令注入内容。");
            }
        }
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse response, AdvisorChain chain) {
        return response;
    }

    @Override
    public String getName() {
        return "SafetyGuardAdvisor";
    }

    @Override
    public int getOrder() {
        // 必须先于会话记忆 Advisor（其默认 order 为 HIGHEST_PRECEDENCE + 1000）执行，
        // 否则被拦截的注入内容会先写入会话历史，下一轮被回放给模型。
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
