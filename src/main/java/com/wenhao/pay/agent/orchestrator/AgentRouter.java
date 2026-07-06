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
            // UNKNOWN 已在编排层短路，走到这里说明新增了 IssueType 却没有注册对应领域 Agent
            throw new IllegalStateException("问题类型未注册对应的领域 Agent：" + type);
        }
        return agent;
    }
}
