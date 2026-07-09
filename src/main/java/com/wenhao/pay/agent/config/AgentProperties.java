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

    public record Tool(Db db, Log log, Channel channel, Mq mq, XxlJob xxljob, Jenkins jenkins, Nacos nacos) {

        public record Db(boolean readonly, List<String> allowedTables) {
        }

        public record Log(int maxResults, int maxTimeRangeDays, Cls cls) {

            /** 腾讯云 CLS 日志服务连接配置。secretId 为空视为未接入，查询时报错提示。 */
            public record Cls(String secretId, String secretKey, String region, String topicId) {
            }
        }

        public record Channel(int timeoutSeconds) {
        }

        /** RocketMQ Admin 连接配置（NameServer 地址）。 */
        public record Mq(String nameServer) {
        }

        /** XXL-JOB 控制台连接配置，仅只读查询。 */
        public record XxlJob(String adminUrl, String username, String password) {
        }

        /** Jenkins 连接配置，仅只读查询发版记录。 */
        public record Jenkins(String url, String username, String apiToken, String jobPath) {
        }

        /** Nacos 配置中心连接配置，仅只读查询。 */
        public record Nacos(String serverAddr, String namespace, String username, String password) {
        }
    }

    /** LLM HTTP 客户端配置（超时等）。 */
    public record Llm(int timeoutSeconds) {
    }
}
