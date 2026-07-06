package com.wenhao.pay.agent.config;

import com.wenhao.pay.agent.advisor.AuditLogAdvisor;
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
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.InMemoryChatMemoryRepository;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Agent 装配中心：为编排 Agent 和 4 个领域 Agent 各自构建一个 ChatClient。
 *
 * 每个领域 Agent = 专属 SOP System Prompt（含领域元数据）+ 专属 Tool 集 + 审计 Advisor。
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
                                         AuditLogAdvisor audit) {
        return domainAgentClient(builder, SystemPrompts.PAYMENT_AGENT, audit,
                payOrderQueryTool, channelQueryTool, logSearchTool, rocketMQStatusTool);
    }

    @Bean("refundAgentClient")
    public ChatClient refundAgentClient(ChatClient.Builder builder,
                                        RefundQueryTool refundQueryTool,
                                        PayOrderQueryTool payOrderQueryTool,
                                        ChannelQueryTool channelQueryTool,
                                        LogSearchTool logSearchTool,
                                        AuditLogAdvisor audit) {
        return domainAgentClient(builder, SystemPrompts.REFUND_AGENT, audit,
                refundQueryTool, payOrderQueryTool, channelQueryTool, logSearchTool);
    }

    @Bean("ledgerAgentClient")
    public ChatClient ledgerAgentClient(ChatClient.Builder builder,
                                        LedgerQueryTool ledgerQueryTool,
                                        PayOrderQueryTool payOrderQueryTool,
                                        ChannelQueryTool channelQueryTool,
                                        LogSearchTool logSearchTool,
                                        AuditLogAdvisor audit) {
        return domainAgentClient(builder, SystemPrompts.LEDGER_AGENT, audit,
                ledgerQueryTool, payOrderQueryTool, channelQueryTool, logSearchTool);
    }

    @Bean("reconciliationAgentClient")
    public ChatClient reconciliationAgentClient(ChatClient.Builder builder,
                                                TxRecordQueryTool txRecordQueryTool,
                                                PayOrderQueryTool payOrderQueryTool,
                                                ChannelQueryTool channelQueryTool,
                                                LogSearchTool logSearchTool,
                                                AuditLogAdvisor audit) {
        return domainAgentClient(builder, SystemPrompts.RECONCILIATION_AGENT, audit,
                txRecordQueryTool, payOrderQueryTool, channelQueryTool, logSearchTool);
    }

    /** 领域 Agent 通用装配：SOP Prompt 末尾拼接公共领域元数据与通用规则。 */
    private ChatClient domainAgentClient(ChatClient.Builder builder, String sopPrompt,
                                         AuditLogAdvisor audit, Object... tools) {
        return builder
                .defaultSystem(sopPrompt + "\n" + SystemPrompts.DOMAIN_CONTEXT)
                .defaultTools(tools)
                .defaultAdvisors(audit)
                .build();
    }

    /** 多轮会话记忆（内存版，按 maxHistory 滑动窗口）。生产可换 Redis 版 ChatMemoryRepository，接口不变。 */
    @Bean
    public ChatMemory chatMemory(AgentProperties properties) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(new InMemoryChatMemoryRepository())
                .maxMessages(properties.session().maxHistory())
                .build();
    }

    /** 会话记忆 Advisor：带 sessionId 的请求在调用时挂载（见 OrchestratorAgent / AbstractDiagnosisAgent）。 */
    @Bean
    public MessageChatMemoryAdvisor chatMemoryAdvisor(ChatMemory chatMemory) {
        return MessageChatMemoryAdvisor.builder(chatMemory).build();
    }

    /** LLM 同步调用超时（Spring AI 底层 RestClient）。 */
    @Bean
    public RestClientCustomizer llmRestClientTimeout(AgentProperties properties) {
        return builder -> builder.requestFactory(ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofSeconds(10))
                        .withReadTimeout(Duration.ofSeconds(properties.llm().timeoutSeconds()))));
    }

    /** LLM 流式调用超时（Spring AI 底层 WebClient）。 */
    @Bean
    public WebClientCustomizer llmWebClientTimeout(AgentProperties properties) {
        return builder -> builder.clientConnector(new ReactorClientHttpConnector(
                HttpClient.create().responseTimeout(Duration.ofSeconds(properties.llm().timeoutSeconds()))));
    }
}
