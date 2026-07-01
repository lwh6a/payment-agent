package com.wenhao.pay.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 支付排障 Agent 启动类。
 *
 * 架构分层（自上而下）：
 * controller(入口) → advisor(横切：审计/安全/上下文) → orchestrator(编排)
 *   → agent(领域排障) → tool(@Tool 工具) → 数据源(库/日志/MQ/渠道)
 */
@SpringBootApplication
public class AgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentApplication.class, args);
    }
}
