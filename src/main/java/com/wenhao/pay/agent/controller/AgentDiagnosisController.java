package com.wenhao.pay.agent.controller;

import com.wenhao.pay.agent.model.dto.DiagnosisRequest;
import com.wenhao.pay.agent.model.dto.DiagnosisResponse;
import com.wenhao.pay.agent.orchestrator.OrchestratorAgent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * REST 排障入口：结构化请求 → 结构化排障结果，供内部工具平台集成。
 * 诊断是阻塞的 LLM 调用，切到 boundedElastic 执行，避免阻塞 Netty event loop。
 */
@RestController
@RequestMapping("/agent")
public class AgentDiagnosisController {

    private final OrchestratorAgent orchestrator;

    public AgentDiagnosisController(OrchestratorAgent orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/diagnose")
    public Mono<DiagnosisResponse> diagnose(@RequestBody DiagnosisRequest request) {
        return Mono.fromCallable(() -> orchestrator.diagnose(request))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
