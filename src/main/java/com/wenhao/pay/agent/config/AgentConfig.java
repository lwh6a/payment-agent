package com.wenhao.pay.agent.config;

import com.wenhao.pay.agent.advisor.AuditLogAdvisor;
import com.wenhao.pay.agent.advisor.ContextEnrichAdvisor;
import com.wenhao.pay.agent.advisor.SafetyGuardAdvisor;
import com.wenhao.pay.agent.prompt.SystemPrompts;
import com.wenhao.pay.agent.tool.channel.ChannelQueryTool;
import com.wenhao.pay.agent.tool.db.LedgerQueryTool;
import com.wenhao.pay.agent.tool.db.PayOrderQueryTool;
import com.wenhao.pay.agent.tool.db.RefundQueryTool;
import com.wenhao.pay.agent.tool.db.TxRecordQueryTool;
import com.wenhao.pay.agent.tool.log.LogSearchTool;
import com.wenhao.pay.agent.tool.mq.RocketMQStatusTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agent 装配中心：为编排 Agent 和 4 个领域 Agent 各自构建一个 ChatClient。
 *
 * 每个领域 Agent = 专属 System Prompt + 专属 Tool 集 + Advisor 链。
 * 编排 Agent 不带 Tool（只分诊），并挂载安全护栏作为统一入口防线。
 *
 * ChatClient.Builder 由 Spring AI 自动配置为 prototype，每个 @Bean 方法都拿到独立实例。
 */
@Configuration
@EnableConfigurationProperties(AgentProperties.class)
public class AgentConfig {

    @Bean("orchestratorClient")
    public ChatClient orchestratorClient(ChatClient.Builder builder,
                                         SafetyGuardAdvisor safetyGuard,
                                         AuditLogAdvisor audit) {
        return builder
                .defaultSystem(SystemPrompts.ORCHESTRATOR)
                .defaultAdvisors(safetyGuard, audit)
                .build();
    }

    @Bean("paymentAgentClient")
    public ChatClient paymentAgentClient(ChatClient.Builder builder,
                                         PayOrderQueryTool payOrderQueryTool,
                                         ChannelQueryTool channelQueryTool,
                                         LogSearchTool logSearchTool,
                                         RocketMQStatusTool rocketMQStatusTool,
                                         ContextEnrichAdvisor contextEnrich,
                                         AuditLogAdvisor audit) {
        return builder
                .defaultSystem(SystemPrompts.PAYMENT_AGENT)
                .defaultTools(payOrderQueryTool, channelQueryTool, logSearchTool, rocketMQStatusTool)
                .defaultAdvisors(contextEnrich, audit)
                .build();
    }

    @Bean("refundAgentClient")
    public ChatClient refundAgentClient(ChatClient.Builder builder,
                                        RefundQueryTool refundQueryTool,
                                        PayOrderQueryTool payOrderQueryTool,
                                        ChannelQueryTool channelQueryTool,
                                        LogSearchTool logSearchTool,
                                        ContextEnrichAdvisor contextEnrich,
                                        AuditLogAdvisor audit) {
        return builder
                .defaultSystem(SystemPrompts.REFUND_AGENT)
                .defaultTools(refundQueryTool, payOrderQueryTool, channelQueryTool, logSearchTool)
                .defaultAdvisors(contextEnrich, audit)
                .build();
    }

    @Bean("ledgerAgentClient")
    public ChatClient ledgerAgentClient(ChatClient.Builder builder,
                                        LedgerQueryTool ledgerQueryTool,
                                        PayOrderQueryTool payOrderQueryTool,
                                        ChannelQueryTool channelQueryTool,
                                        LogSearchTool logSearchTool,
                                        ContextEnrichAdvisor contextEnrich,
                                        AuditLogAdvisor audit) {
        return builder
                .defaultSystem(SystemPrompts.LEDGER_AGENT)
                .defaultTools(ledgerQueryTool, payOrderQueryTool, channelQueryTool, logSearchTool)
                .defaultAdvisors(contextEnrich, audit)
                .build();
    }

    @Bean("reconciliationAgentClient")
    public ChatClient reconciliationAgentClient(ChatClient.Builder builder,
                                                TxRecordQueryTool txRecordQueryTool,
                                                PayOrderQueryTool payOrderQueryTool,
                                                ChannelQueryTool channelQueryTool,
                                                LogSearchTool logSearchTool,
                                                ContextEnrichAdvisor contextEnrich,
                                                AuditLogAdvisor audit) {
        return builder
                .defaultSystem(SystemPrompts.RECONCILIATION_AGENT)
                .defaultTools(txRecordQueryTool, payOrderQueryTool, channelQueryTool, logSearchTool)
                .defaultAdvisors(contextEnrich, audit)
                .build();
    }
}
