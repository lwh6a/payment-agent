package com.wenhao.pay.agent.orchestrator;

import com.wenhao.pay.agent.agent.DiagnosisAgent;
import com.wenhao.pay.agent.model.dto.DiagnosisRequest;
import com.wenhao.pay.agent.model.dto.DiagnosisResponse;
import com.wenhao.pay.agent.model.dto.IssueClassification;
import com.wenhao.pay.agent.model.enums.DiagnosisStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 编排 Agent：排障入口。
 *
 * 流程：意图识别（结构化抽取问题类型 + 参数）→ 路由到领域 Agent → 领域 Agent 调工具诊断 → 汇总返回。
 * 编排层自身不查数据、不持有 Tool，只做"分诊台"。
 */
@Component
public class OrchestratorAgent {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorAgent.class);

    private final ChatClient orchestratorClient;
    private final AgentRouter router;

    public OrchestratorAgent(@Qualifier("orchestratorClient") ChatClient orchestratorClient,
                             AgentRouter router) {
        this.orchestratorClient = orchestratorClient;
        this.router = router;
    }

    /** 同步排障。 */
    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        IssueClassification c = classify(request.question());
        log.info("[编排] 识别类型={} tradeNo={}", c.type(), c.tradeNo());
        DiagnosisAgent agent = router.route(c.type());
        String report = agent.diagnose(request.question(), c);
        return new DiagnosisResponse(c.type(), DiagnosisStatus.COMPLETED, c.tradeNo(), report);
    }

    /** 流式排障（SSE）。 */
    public Flux<String> diagnoseStream(String question) {
        IssueClassification c = classify(question);
        log.info("[编排-流式] 识别类型={} tradeNo={}", c.type(), c.tradeNo());
        return router.route(c.type()).diagnoseStream(question, c);
    }

    /** 意图识别：用结构化输出直接拿到分类结果。 */
    private IssueClassification classify(String question) {
        return orchestratorClient.prompt()
                .user(question)
                .call()
                .entity(IssueClassification.class);
    }
}
