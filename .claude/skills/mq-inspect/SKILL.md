---
name: mq-inspect
description: 通过 RocketMQ 控制台查询 Topic、消费组堆积、按时间/业务Key检索消息、查看单条消息与消费轨迹。触发词：查消息、MQ 消费了没、消息堆积、这条消息发出去没有。依赖 devops-config.json 的 rocketmq 段，脚本只用 Python 标准库。
---

# RocketMQ 消息取证

## 什么时候用

- 消息类问题取证：消息有没有发出去、有没有被消费、消费是否失败
- 排查消费延迟：看消费组堆积量（diffTotal）
- 按业务 Key（交易号等）反查一条消息的完整轨迹

## 使用方式

```bash
# Topic 列表（默认过滤掉 %RETRY% / %DLQ%，--all 显示全部）
python3 .claude/skills/mq-inspect/scripts/rocketmq.py topics --filter pay

# 消费组列表（含在线实例数、TPS、堆积量）
python3 .claude/skills/mq-inspect/scripts/rocketmq.py groups

# 某消费组的消费详情
python3 .claude/skills/mq-inspect/scripts/rocketmq.py stats --group GID_pay_consumer

# 按时间段查消息（默认最近3小时）
python3 .claude/skills/mq-inspect/scripts/rocketmq.py search --topic pay_callback_topic --hours 1

# 按业务 Key 查消息（推荐，用交易号等唯一键）
python3 .claude/skills/mq-inspect/scripts/rocketmq.py key --topic pay_callback_topic --key "T2026070100001"

# 查看单条消息详情 + 各消费组的消费轨迹
python3 .claude/skills/mq-inspect/scripts/rocketmq.py view --topic pay_callback_topic --msg-id "0A1B2C..."
```

脚本从当前目录 `devops-config.json` 读取 `rocketmq` 段（consoleUrl，开了登录的控制台再配 username / password，模板见 `devops-config.example.json`）；可用环境变量 `DEVOPS_CONFIG` 指定路径。

## 排查思路（AI 参考）

1. 先 `key` 按业务键查消息 → 查不到 = 生产端没发出来，去查生产端代码/日志。
2. 查到了 → `view` 看消费轨迹 → NOT_CONSUME_YET = 没消费；CONSUMED_BUT_FILTERED = 被过滤；FAILED = 消费失败。
3. 消费失败/延迟 → `groups` 看堆积量，`stats` 看具体消费组情况。

## 已知注意事项

- **不同版本 rocketmq-console / rocketmq-dashboard 的接口路径可能有差异**。调不通时浏览器打开控制台，从 Network 面板确认实际接口路径后调整脚本。
- 消费轨迹依赖 broker 开启 trace（traceTopicEnable）；未开启时查不到轨迹，只能靠堆积量和消费位点间接判断。
- 本 Skill 全部是只读查询，无写操作。重发消息、重置位点等操作不在本 Skill 范围内，需人工在控制台操作。
