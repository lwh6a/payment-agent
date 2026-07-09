# payment-agent · 支付排障 Agent

基于 **Spring AI 1.0 + 多 Agent 架构**的支付结算智能排障系统。把"人工查库 → 翻日志 → 逐链路追踪"的排障 SOP，固化成由大模型驱动、可自然语言对话的自动诊断能力。

> 本工程是**可运行骨架**：流程、分层、排障 SOP System Prompt、Function Calling、Orchestrator 编排、多 Agent、日志切面均已写实。
> Tool 层的 DB / 日志(CLS) / MQ / 定时任务 / 发版记录 / Nacos 配置查询均已实现（SQL 与字段按占位表结构编写，接入真实环境时调整映射）；**渠道查询依赖各渠道 SDK，留待接入**。

## 一、请求流程

```
用户提问（REST / SSE）
   │
   ▼  AuditLogAdvisor（审计） + SafetyGuardAdvisor（安全护栏，仅入口）
OrchestratorAgent  ── 意图识别 + 参数抽取 + 危险意图判定（结构化输出 IssueClassification）
   │
   ▼  AgentRouter 按 IssueType 路由（UNKNOWN 直接返回引导语，不进路由）
领域 Agent（支付 / 退款 / 分账 / 对账）
   │  ── 按 System Prompt 里的排障决策链，自主决定调用哪些 @Tool
   ▼  ToolQueryGuard（只读/表白名单/限额校验） + ToolCallLogAspect（逐次记录工具调用）
Tool 层（DB / 日志 / MQ / 定时任务 / 发版记录 / 配置中心 / 渠道，全部只读）
   │
   ▼
数据源（MySQL / CLS / RocketMQ / XXL-JOB / Jenkins / Nacos / 渠道 API）
   │
   ▼
结构化排障报告
```

## 二、分层与目录

| 层 | 包 | 职责 |
|---|---|---|
| 入口 | `controller` | REST `/agent/diagnose`、SSE `/agent/chat`、全局异常 |
| 横切 | `advisor` / `aspect` | 审计日志（含 Token 用量）、安全护栏 Advisor；Tool 调用日志切面 |
| 编排 | `orchestrator` | `OrchestratorAgent` 分诊 + `AgentRouter` 路由 |
| 领域 Agent | `agent` | 4 个领域 Agent，各持专属 Prompt + Tool 集 |
| 装配 | `config` | `AgentConfig` 构建 5 个 ChatClient；`AgentProperties` 配置 |
| 提示词 | `prompt` | `SystemPrompts`：排障 SOP（状态机 + 决策链） |
| 工具 | `tool/{db,log,mq,job,deploy,nacos,channel}` | `@Tool` 数据访问（channel 待接入）；`ToolQueryGuard` 白名单/限额防护 |
| 会话 | `config`（ChatMemory Bean） | Spring AI `MessageChatMemoryAdvisor` 多轮记忆（内存版，可切 Redis） |
| 模型 | `model/{dto,enums}` | 请求/响应、分类结果、各领域占位 DTO |

## 三、核心设计点

1. **Function Calling 是骨架**：每个排查动作封成 `@Tool` 方法，模型只能"点菜"调工具、不碰库，拿结构化结果再推理 —— 可控、安全、确定性高。
2. **多 Agent 而非单 Agent**：拆领域后每个 Agent 工具集小、决策链清晰，可独立授权、可并行扩展。
3. **SOP 写进 System Prompt**：`SystemPrompts` 内置支付/退款/分账状态机与决策链，让模型按专家步骤走。
4. **安全护栏（支付场景底线，三层防线）**：
   ① Tool 层只读 + 表白名单——`ToolQueryGuard` 启动校验 `readonly=true`、查询前校验白名单，模型没有任何写能力（硬边界）；
   ② 编排意图识别输出 `unsafe` 字段——语义判定改单/转账等危险请求，命中直接 403；
   ③ `SafetyGuardAdvisor` 拦截提示注入特征词。另有 `temperature=0.1` 与工具调用预算（单次诊断 ≤ 8 次）。

## 四、运行

```bash
# 必填：LLM
export LLM_API_KEY=sk-xxx
export LLM_BASE_URL=https://api.openai.com   # 可换成兼容网关
export LLM_MODEL=gpt-4o

# 按需：数据源与基础设施（不配置也能启动，对应工具查询时会提示未接入）
export DB_URL='jdbc:mysql://localhost:3306/pay_core?useSSL=false&serverTimezone=Asia/Shanghai'
export DB_USERNAME=readonly DB_PASSWORD=xxx            # 务必使用只读账号
export CLS_SECRET_ID=xxx CLS_SECRET_KEY=xxx CLS_REGION=ap-guangzhou CLS_TOPIC_ID=xxx
export MQ_NAME_SERVER=localhost:9876
export XXLJOB_ADMIN_URL=http://localhost:8082/xxl-job-admin XXLJOB_USERNAME=admin XXLJOB_PASSWORD=xxx
export JENKINS_URL=http://localhost:8083 JENKINS_USERNAME=xxx JENKINS_API_TOKEN=xxx JENKINS_JOB_PATH=payment-agent
export NACOS_SERVER_ADDR=http://localhost:8848 NACOS_NAMESPACE=xxx

mvn spring-boot:run
```

```bash
# REST（sessionId 可选，传了即启用多轮会话记忆）
curl -X POST http://localhost:8088/agent/diagnose \
  -H 'Content-Type: application/json' \
  -d '{"question":"订单 T20240101001 支付后一直没回调","sessionId":"s-001"}'

# SSE（同一 sessionId 可继续追问，如"渠道侧状态是什么"）
curl -N 'http://localhost:8088/agent/chat?question=退款单R001两天还在退款中&sessionId=s-001'
```

## 五、接入真实环境时要做的事

1. **DB 工具**（`tool/db`）：SQL 与字段映射按占位表结构编写，对照真实表结构调整列名 / 状态码映射（`PayStates`）。
2. **渠道工具**（`tool/channel`）：唯一留空的部分，依赖各渠道 SDK/适配器，按 payMode 路由接入。
3. **日志工具**（`tool/log`）：已对接腾讯云 CLS；日志字段名做了常见别名兼容，接入后按实际日志格式精简。
4. **对账比对**（`batchCompareOrders`）：当前以清算记录为渠道侧对照，接入渠道对账文件后替换数据源。

## 六、开发工作流（.claude/）

本仓库自带面向 Claude Code 的开发工作流（详见 [CLAUDE.md](CLAUDE.md)）：

- **取证 skill**：mysql-query / cls-logs / mq-inspect / xxljob-ops——开发排障时一句话查库、查日志、查消息、查任务；
- **发版 skill**：jenkins-deploy（触发构建 + 轮询）+ deploy-checklist（按改动生成 SIT/PROD 检查单）；
- **闭环 skill**：bug-fix-workflow——读 Bug → 取证 → 修复 → 发版 → 日志验证 → 回写工单；
- **质量 skill**：diagnosis-regression——badcase 用例回放，改 Prompt / 换模型后防退化；
- **云效 MCP**：`.mcp.json` 已配置需求/Bug 读写（需 `YUNXIAO_ACCESS_TOKEN`）。

连接配置放项目根目录 `devops-config.json`（已 gitignore），模板见 `devops-config.example.json`。

## 七、版本说明

针对 **Spring AI 1.0.1**（`ChatClient` / `@Tool` / `BaseAdvisor` / `ChatMemory`）。若依赖版本不同，Advisor 的 `ChatClientRequest/Response` 等 API 可能需按所用版本微调。
已接入 actuator：`/actuator/metrics` 下可看 Spring AI 自带的 ChatClient / Tool 调用指标（`gen_ai.*`）。
