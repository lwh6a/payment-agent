package com.wenhao.pay.agent.model.dto;

import com.wenhao.pay.agent.model.enums.IssueType;

/**
 * Orchestrator 意图识别的结构化产出：问题类型 + 抽取出的关键参数 + 危险意图判定。
 * 由大模型按结构化输出格式自动填充（Spring AI .entity(IssueClassification.class)）。
 * unsafe=true 表示用户在请求写操作（改单/转账/删除等）或试图绕过系统规则，编排层直接拒绝。
 */
public record IssueClassification(
        IssueType type,
        String tradeNo,
        String orderNo,
        String merchantCode,
        String summary,
        boolean unsafe
) {
}
