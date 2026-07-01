package com.wenhao.pay.agent.model.dto;

import com.wenhao.pay.agent.model.enums.DiagnosisStatus;
import com.wenhao.pay.agent.model.enums.IssueType;

/**
 * 排障结果。report 为领域 Agent 输出的结构化可读排障报告。
 */
public record DiagnosisResponse(
        IssueType type,
        DiagnosisStatus status,
        String tradeNo,
        String report
) {
}
