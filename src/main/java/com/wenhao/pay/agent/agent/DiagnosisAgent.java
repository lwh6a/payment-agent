package com.wenhao.pay.agent.agent;

import com.wenhao.pay.agent.model.dto.IssueClassification;
import com.wenhao.pay.agent.model.enums.IssueType;
import reactor.core.publisher.Flux;

/**
 * 领域排障 Agent 统一接口。每个实现对应一类问题，持有自己的 ChatClient（专属 Prompt + 专属 Tool 集）。
 */
public interface DiagnosisAgent {

    /** 该 Agent 处理的问题类型，用于路由。 */
    IssueType supports();

    /** 同步排障，返回完整排障报告。sessionId 非空时启用多轮会话记忆。 */
    String diagnose(String question, IssueClassification classification, String sessionId);

    /** 流式排障，逐段返回（用于 SSE 对话）。sessionId 非空时启用多轮会话记忆。 */
    Flux<String> diagnoseStream(String question, IssueClassification classification, String sessionId);
}
