package com.wenhao.pay.agent.orchestrator;

import com.wenhao.pay.agent.agent.DiagnosisAgent;
import com.wenhao.pay.agent.model.enums.IssueType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Agent 路由：按问题类型找到对应领域 Agent。
 * 自动收集所有 DiagnosisAgent Bean，新增 Agent 无需改路由。
 */
@Component
public class AgentRouter {

    private final Map<IssueType, DiagnosisAgent> agentMap;

    public AgentRouter(List<DiagnosisAgent> agents) {
        this.agentMap = agents.stream()
                .collect(Collectors.toMap(DiagnosisAgent::supports, Function.identity()));
    }

    public DiagnosisAgent route(IssueType type) {
        DiagnosisAgent agent = agentMap.get(type);
        if (agent == null) {
            throw new IllegalArgumentException("无法识别的问题类型，建议转人工排查：" + type);
        }
        return agent;
    }
}
