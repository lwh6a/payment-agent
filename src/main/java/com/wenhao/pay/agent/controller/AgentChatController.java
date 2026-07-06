package com.wenhao.pay.agent.controller;

import com.wenhao.pay.agent.orchestrator.OrchestratorAgent;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat 流式排障入口（SSE）：自然语言提问，逐段返回排障过程与结论。
 * 传 sessionId 即启用多轮会话记忆（ChatMemory），支持追问。
 */
@RestController
@RequestMapping("/agent")
public class AgentChatController {

    private final OrchestratorAgent orchestrator;

    public AgentChatController(OrchestratorAgent orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question,
                             @RequestParam(required = false) String sessionId) {
        return orchestrator.diagnoseStream(question, sessionId);
    }
}
