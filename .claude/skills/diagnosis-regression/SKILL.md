---
name: diagnosis-regression
description: 诊断质量回归：把历史 badcase 固化成用例，修改 Prompt / 更换模型 / 调整工具后回放 /agent/diagnose，验证诊断结论不退化。触发词：跑回归、回归诊断用例、验证 Prompt 改动。
---

# 诊断质量回归

payment-agent 的排障 SOP 写在 System Prompt 里，改一个词都可能影响诊断路径。本 skill 把真实排障 badcase 固化成「问题 → 期望结论关键词」用例集，改动后回放验证。

## 什么时候用

- 修改 `SystemPrompts` 的任何内容之后
- 更换 LLM 模型（`LLM_MODEL`）之后
- 新增/调整 `@Tool` 工具集之后

## 使用方式

```bash
# 先在本地把服务跑起来（需要 LLM_API_KEY 与对应环境数据）
mvn spring-boot:run

# 回放全部用例
python3 .claude/skills/diagnosis-regression/scripts/replay.py

# 只跑单个用例 / 指定服务地址
python3 .claude/skills/diagnosis-regression/scripts/replay.py --case pay-001 --base-url http://localhost:8088
```

## 用例格式（cases/cases.json）

```json
{
  "id": "pay-001",
  "type": "PAYMENT",
  "question": "订单 T2026070100001 支付后一直没回调",
  "expect_keywords": ["PAYING", "回调"],
  "note": "关键词按期望出现在诊断报告中的根因/结论词设计"
}
```

判定标准：诊断报告文本包含用例的**全部** `expect_keywords` 即通过。

## 沉淀约定

- 每次线上排障发现模型诊断错误（badcase），修完后**把该问题追加为一条用例**，问题原文 + 正确根因关键词。
- 用例依赖环境里的真实数据，交易号等参数换环境后需同步调整。
- 仓库自带的用例是格式示例，接入真实环境后替换。
