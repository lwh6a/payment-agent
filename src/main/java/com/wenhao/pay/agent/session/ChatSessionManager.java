package com.wenhao.pay.agent.session;

import com.wenhao.pay.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * 多轮会话上下文管理（内存版）。按 sessionId 保留最近 maxHistory 条消息。
 * 生产可替换为 Redis 存储以支持多实例共享，接口不变。
 */
@Component
public class ChatSessionManager {

    private final Map<String, Deque<String>> store = new ConcurrentHashMap<>();
    private final int maxHistory;

    public ChatSessionManager(AgentProperties properties) {
        this.maxHistory = properties.session().maxHistory();
    }

    public void append(String sessionId, String role, String content) {
        Deque<String> history = store.computeIfAbsent(sessionId, k -> new ConcurrentLinkedDeque<>());
        history.addLast(role + ": " + content);
        while (history.size() > maxHistory) {
            history.pollFirst();
        }
    }

    public List<String> history(String sessionId) {
        return new ArrayList<>(store.getOrDefault(sessionId, new ConcurrentLinkedDeque<>()));
    }
}
