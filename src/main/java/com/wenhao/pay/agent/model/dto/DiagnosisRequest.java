package com.wenhao.pay.agent.model.dto;

/**
 * 排障请求。question 为自然语言描述，sessionId 用于多轮对话（可选）。
 */
public record DiagnosisRequest(
        String question,
        String sessionId
) {
}
