# payment-agent · 支付排障 Agent

基于 **Spring AI 1.0 + 多 Agent 架构**的支付结算智能排障系统。把"人工查库 → 翻日志 → 逐链路追踪"的排障 SOP，固化成由大模型驱动、可自然语言对话的自动诊断能力。

> 本工程是**完整骨架**：流程、分层、排障 SOP System Prompt、Function Calling、Orchestrator 编排、多 Agent、日志切面均已写实。
> **Tool 层只给方法签名（`@Tool` 描述齐全），方法体留 `TODO` 由你对接对应的 Service 实现。**

## 一、请求流程

```
用户提问（REST / SSE）
   │
   ▼  AuditLogAdvisor（审计） + SafetyGuardAdvisor（安全护栏，仅入口）
OrchestratorAgent  ── 意图识别 + 参数抽取（结构化输出 IssueClassification）
   │
   ▼  AgentRouter 按 IssueType 路由
领域 Agent（支付 / 退款 / 分账 / 对账）
   │  ── 按 System Prompt 里的排障决策链，自主决定调用哪些 @Tool
   ▼  ContextEnrichAdvisor（注入状态机元数据） + ToolCallLogAspect（逐次记录工具调用）
Tool 层（DB / 日志 / MQ / 渠道，全部只读）
   │
   ▼
数据源（MySQL / CLS·ELK / RocketMQ / 渠道 API）
   │
   ▼
结构化排障报告
```

## 二、分层与目录

| 层 | 包 | 职责 |
|---|---|---|
| 入口 | `controller` | REST `/agent/diagnose`、SSE `/agent/chat`、全局异常 |
| 横切 | `advisor` / `aspect` | 审计日志、安全护栏、上下文增强 Advisor；Tool 调用日志切面 |
| 编排 | `orchestrator` | `OrchestratorAgent` 分诊 + `AgentRouter` 路由 |
| 领域 Agent | `agent` | 4 个领域 Agent，各持专属 Prompt + Tool 集 |
| 装配 | `config` | `AgentConfig` 构建 5 个 ChatClient；`AgentProperties` 配置 |
| 提示词 | `prompt` | `SystemPrompts`：排障 SOP（状态机 + 决策链） |
| 工具 | `tool/{db,log,mq,channel}` | `@Tool` 数据访问（**待实现**） |
| 会话 | `session` | 多轮上下文（内存版，可切 Redis） |
| 模型 | `model/{dto,enums}` | 请求/响应、分类结果、各领域占位 DTO |

## 三、核心设计点

1. **Function Calling 是骨架**：每个排查动作封成 `@Tool` 方法，模型只能"点菜"调工具、不碰库，拿结构化结果再推理 —— 可控、安全、确定性高。
2. **多 Agent 而非单 Agent**：拆领域后每个 Agent 工具集小、决策链清晰，可独立授权、可并行扩展。
3. **SOP 写进 System Prompt**：`SystemPrompts` 内置支付/退款/分账状态机与决策链，让模型按专家步骤走。
4. **安全护栏（支付场景底线）**：数据库只读 + 表白名单（`application.yml`），渠道只调查询方法，`SafetyGuardAdvisor` 拦截改单/转账/注入意图，`temperature=0.1`。

## 四、运行

```bash
export LLM_API_KEY=sk-xxx
export LLM_BASE_URL=https://api.openai.com   # 可换成兼容网关
export LLM_MODEL=gpt-4o
mvn spring-boot:run
```

```bash
# REST
curl -X POST http://localhost:8088/agent/diagnose \
  -H 'Content-Type: application/json' \
  -d '{"question":"订单 T20240101001 支付后一直没回调"}'

# SSE
curl -N 'http://localhost:8088/agent/chat?question=退款单R001两天还在退款中'
```

## 五、待实现（你来做）

逐个补 `tool/**` 下 `@Tool` 方法体：注入对应只读 Service，查询后转成 `model/dto` 里的 DTO 返回。占位 DTO 字段按真实表结构调整。

## 六、版本说明

针对 **Spring AI 1.0.0 GA**（`ChatClient` / `@Tool` / `BaseAdvisor`）。若依赖版本不同，Advisor 的 `ChatClientRequest/Response`、`request.mutate()` 等 API 可能需按所用版本微调。
