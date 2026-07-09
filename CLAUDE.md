# payment-agent

基于 Spring AI 1.0 的支付排障多 Agent 服务（Java 21 / Spring Boot 3.3）。整体架构、请求流程与分层见 [README.md](README.md)。

## 常用命令

- 编译：`mvn -q compile`
- 运行：`LLM_API_KEY=sk-xxx mvn spring-boot:run`（全部环境变量见 README「运行」一节）
- 本仓库约定：改动只要求编译通过，不新增测试用例、不跑测试。

## 代码约定

- 代码针对 **Spring AI 1.0.1** 的 `ChatClient` / `@Tool` / `BaseAdvisor` / `ChatMemory` API 编写，升级依赖时注意 Advisor 的 `ChatClientRequest/Response` 等 API 兼容性。
- **Tool 层是只读底线**：新增任何 `@Tool` 必须只读；DB 查询先过 `ToolQueryGuard` 表白名单；外部系统（MQ / XXL-JOB / Jenkins / Nacos）只接查询接口，不接触发/写入接口。
- 金额库内为「分」，输出给模型/用户时转「元」。
- 新增配置统一放 `AgentProperties`（`agent.*` 前缀），敏感值一律走环境变量，不写死在 yml。
- `@Tool` 方法上的调用日志由 `ToolCallLogAspect` 按注解自动切入，新增工具无需改切面。

## 开发工作流（.claude/skills/）

| skill | 用途 |
|---|---|
| mysql-query | 只读查库取证 |
| cls-logs | 查 CLS 日志（报错取证、发版后验证） |
| mq-inspect | RocketMQ 消息/堆积/轨迹取证 |
| xxljob-ops | XXL-JOB 任务与执行日志查询、手动触发 |
| jenkins-deploy | 触发构建发版、查构建状态和日志 |
| bug-fix-workflow | Bug 修复闭环：读 Bug → 取证 → 修复 → 发版 → 日志验证 → 回写状态 |
| deploy-checklist | 按本次改动自动生成 SIT/PROD 发版检查单 |
| diagnosis-regression | 诊断质量回归：badcase 用例回放，防止 Prompt/模型改动导致退化 |

- 取证/发版类 skill 的连接信息统一放项目根目录 `devops-config.json`（已 gitignore），模板见 [devops-config.example.json](devops-config.example.json)。
- Python 依赖：`pip3 install --user -r .claude/skills/requirements.txt`。
- 云效 MCP：[.mcp.json](.mcp.json) 已配置（需求/Bug 读写、任务跟踪），需要环境变量 `YUNXIAO_ACCESS_TOKEN`。

## 公开仓库红线

本仓库公开在 GitHub，任何提交**不得包含公司相关信息**：

- 内部域名 / IP / 控制台地址；
- 内部系统名、渠道名、内部编码映射、工单号前缀；
- 真实商户号、密钥、Token、账号密码。

`devops-config.json` 与一切凭证文件禁止入库；示例、注释、文档一律使用通用占位值。
