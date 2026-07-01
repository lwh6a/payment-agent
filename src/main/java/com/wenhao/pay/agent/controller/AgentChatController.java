package com.wenhao.pay.agent.controller;

import com.wenhao.pay.agent.orchestrator.OrchestratorAgent;
import com.wenhao.pay.agent.session.ChatSessionManager;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * Chat 流式排障入口（SSE）：自然语言提问，逐段返回排障过程与结论。
 */
@RestController
@RequestMapping("/agent")
public class AgentChatController {

    private final OrchestratorAgent orchestrator;
    private final ChatSessionManager sessions;

    public AgentChatController(OrchestratorAgent orchestrator, ChatSessionManager sessions) {
        this.orchestrator = orchestrator;
        this.sessions = sessions;
    }

    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestParam String question,
                             @RequestParam(required = false) String sessionId) {
        if (StringUtils.hasText(sessionId)) {
            sessions.append(sessionId, "user", question);
        }
        return orchestrator.diagnoseStream(question);
    }
}
