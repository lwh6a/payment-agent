package com.wenhao.pay.agent.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 安全护栏 Advisor：在请求进入模型前拦截危险意图。
 *
 * 排障 Agent 的底线是「只读」——禁止任何修改数据、转账、改单状态、删除等意图，
 * 同时拦截常见的 SQL 注入 / 提示注入关键词。命中即抛异常中断本次请求。
 */
@Component
public class SafetyGuardAdvisor implements BaseAdvisor {

    private static final Logger log = LoggerFactory.getLogger(SafetyGuardAdvisor.class);

    /** 危险意图关键词（命中即拦截）。 */
    private static final List<String> BLOCK_WORDS = List.of(
            "删除", "drop ", "delete ", "truncate", "update ", "insert ",
            "改成", "修改状态", "改状态", "强制成功", "置为成功",
            "转账", "打款", "退钱给我", "提现到",
            "忽略以上", "ignore previous", "system prompt", "你现在是"
    );

    @Override
    public ChatClientRequest before(ChatClientRequest request, AdvisorChain chain) {
        var userMessage = request.prompt().getUserMessage();
        String text = userMessage == null ? "" : userMessage.getText().toLowerCase();
        for (String word : BLOCK_WORDS) {
            if (text.contains(word)) {
                log.warn("[安全护栏-拦截] 命中关键词={} 原文={}", word, text);
                throw new UnsafeRequestException("请求被安全护栏拦截：排障 Agent 仅支持只读查询，不能执行修改/转账等操作。");
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
        return 0; // 最先执行
    }
}
