package com.wenhao.pay.agent.controller;

import com.wenhao.pay.agent.model.dto.DiagnosisRequest;
import com.wenhao.pay.agent.model.dto.DiagnosisResponse;
import com.wenhao.pay.agent.orchestrator.OrchestratorAgent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST 排障入口：结构化请求 → 结构化排障结果，供内部工具平台集成。
 */
@RestController
@RequestMapping("/agent")
public class AgentDiagnosisController {

    private final OrchestratorAgent orchestrator;

    public AgentDiagnosisController(OrchestratorAgent orchestrator) {
        this.orchestrator = orchestrator;
    }

    @PostMapping("/diagnose")
    public DiagnosisResponse diagnose(@RequestBody DiagnosisRequest request) {
        return orchestrator.diagnose(request);
    }
}
