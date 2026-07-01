package com.wenhao.pay.agent.model.dto;

import com.wenhao.pay.agent.model.enums.IssueType;

/**
 * Orchestrator 意图识别的结构化产出：问题类型 + 抽取出的关键参数。
 * 由大模型按结构化输出格式自动填充（Spring AI .entity(IssueClassification.class)）。
 */
public record IssueClassification(
        IssueType type,
        String tradeNo,
        String orderNo,
        String merchantCode,
        String summary
) {
}
