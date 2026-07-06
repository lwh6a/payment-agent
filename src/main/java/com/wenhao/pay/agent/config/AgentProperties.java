package com.wenhao.pay.agent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Agent 配置（对应 application.yml 的 agent.*）。
 * 安全相关项（只读、表白名单、限流）由 {@code ToolQueryGuard} 统一消费。
 */
@ConfigurationProperties(prefix = "agent")
public record AgentProperties(Session session, Tool tool, Llm llm) {

    public record Session(int maxHistory) {
    }

    public record Tool(Db db, Log log, Channel channel) {

        public record Db(boolean readonly, List<String> allowedTables) {
        }

        public record Log(int maxResults, int maxTimeRangeDays) {
        }

        public record Channel(int timeoutSeconds) {
        }
    }

    /** LLM HTTP 客户端配置（超时等）。 */
    public record Llm(int timeoutSeconds) {
    }
}
