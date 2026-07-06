package com.wenhao.pay.agent.orchestrator;

import com.wenhao.pay.agent.advisor.UnsafeRequestException;
import com.wenhao.pay.agent.model.dto.DiagnosisRequest;
import com.wenhao.pay.agent.model.dto.DiagnosisResponse;
import com.wenhao.pay.agent.model.dto.IssueClassification;
import com.wenhao.pay.agent.model.enums.DiagnosisStatus;
import com.wenhao.pay.agent.model.enums.IssueType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 编排 Agent：排障入口。
 *
 * 流程：意图识别（结构化抽取问题类型 + 参数）→ 路由到领域 Agent → 领域 Agent 调工具诊断 → 汇总返回。
 * 编排层自身不查数据、不持有 Tool，只做"分诊台"。
 *
 * 兜底策略：意图识别失败或识别为 UNKNOWN 时不进入路由，直接返回引导语；
 * 领域诊断异常时返回 FAILED 状态而不是抛 500。
 */
@Component
public class OrchestratorAgent {

    private static final Logger log = LoggerFactory.getLogger(OrchestratorAgent.class);

    /** 编排层与领域 Agent 的会话历史分开存，避免分类 JSON 混进诊断上下文。 */
    private static final String TRIAGE_CONVERSATION_SUFFIX = ":triage";

    private static final String UNKNOWN_REPLY = """
            未能识别问题类型。请补充以下信息后重试：
            1. 异常现象属于哪类：支付 / 退款 / 分账 / 对账；
            2. 关键单号：交易号 tradeNo、订单号 orderNo 或商户号 merchantCode。
            若仍无法定位，建议转人工排查。""";

    private final ChatClient orchestratorClient;
    private final MessageChatMemoryAdvisor memoryAdvisor;
    private final AgentRouter router;

    public OrchestratorAgent(@Qualifier("orchestratorClient") ChatClient orchestratorClient,
                             MessageChatMemoryAdvisor memoryAdvisor,
                             AgentRouter router) {
        this.orchestratorClient = orchestratorClient;
        this.memoryAdvisor = memoryAdvisor;
        this.router = router;
    }

    /** 同步排障。领域诊断异常时返回 FAILED，不向外抛。 */
    public DiagnosisResponse diagnose(DiagnosisRequest request) {
        IssueClassification c = classify(request.question(), request.sessionId());
        if (c.type() == IssueType.UNKNOWN) {
            return new DiagnosisResponse(IssueType.UNKNOWN, DiagnosisStatus.FAILED, c.tradeNo(), UNKNOWN_REPLY);
        }
        log.info("[编排] 识别类型={} tradeNo={}", c.type(), c.tradeNo());
        try {
            String report = router.route(c.type()).diagnose(request.question(), c, request.sessionId());
            return new DiagnosisResponse(c.type(), DiagnosisStatus.COMPLETED, c.tradeNo(), report);
        } catch (Exception e) {
            log.error("[编排] 领域诊断失败 type={}", c.type(), e);
            return new DiagnosisResponse(c.type(), DiagnosisStatus.FAILED, c.tradeNo(), "诊断失败：" + e.getMessage());
        }
    }

    /**
     * 流式排障（SSE）。意图识别是阻塞调用，切到 boundedElastic 执行，避免阻塞 Netty event loop。
     * 安全护栏拦截继续向外抛（由全局异常处理返回 403），其余异常转为流内的失败提示。
     */
    public Flux<String> diagnoseStream(String question, String sessionId) {
        return Mono.fromCallable(() -> classify(question, sessionId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(c -> {
                    if (c.type() == IssueType.UNKNOWN) {
                        return Flux.just(UNKNOWN_REPLY);
                    }
                    log.info("[编排-流式] 识别类型={} tradeNo={}", c.type(), c.tradeNo());
                    return router.route(c.type()).diagnoseStream(question, c, sessionId);
                })
                .onErrorResume(e -> {
                    if (e instanceof UnsafeRequestException) {
                        return Flux.error(e);
                    }
                    log.error("[编排-流式] 诊断失败", e);
                    return Flux.just("诊断失败：" + e.getMessage());
                });
    }

    /**
     * 意图识别：用结构化输出直接拿到分类结果。
     * 带 sessionId 时挂载会话记忆，多轮追问（如"那退款单 R001 呢"）也能正确分类。
     * 模型输出解析失败按 UNKNOWN 兜底；判定为危险意图或安全护栏拦截时上抛（全局异常处理转 403）。
     */
    private IssueClassification classify(String question, String sessionId) {
        ChatClient.ChatClientRequestSpec spec = orchestratorClient.prompt().user(question);
        if (StringUtils.hasText(sessionId)) {
            spec = spec.advisors(memoryAdvisor)
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, sessionId + TRIAGE_CONVERSATION_SUFFIX));
        }
        try {
            IssueClassification c = spec.call().entity(IssueClassification.class);
            if (c == null || c.type() == null) {
                return unknown();
            }
            if (c.unsafe()) {
                log.warn("[编排] 意图判定为危险请求：{}", c.summary());
                throw new UnsafeRequestException("请求被拒绝：排障 Agent 仅支持只读查询，不能执行修改/转账等操作。");
            }
            return c;
        } catch (UnsafeRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[编排] 意图识别失败，按 UNKNOWN 处理：{}", e.getMessage());
            return unknown();
        }
    }

    private IssueClassification unknown() {
        return new IssueClassification(IssueType.UNKNOWN, null, null, null, null, false);
    }
}
