---
name: xxljob-ops
description: 查询 XXL-JOB 定时任务列表、执行日志，必要时手动触发一次任务。触发词：查定时任务、job 跑了没、任务执行日志、手动跑一次 job。依赖 devops-config.json 的 xxljob 段，脚本只用 Python 标准库。
---

# XXL-JOB 任务查询与触发

## 什么时候用

- 定时任务类问题取证：任务有没有跑、什么时候跑的、执行结果是成功还是失败
- 修复发版后需要立即验证任务逻辑，手动触发一次

## 使用方式

```bash
# 执行器（分组）列表
python3 .claude/skills/xxljob-ops/scripts/xxljob.py groups

# 任务列表（--group 按执行器过滤，0 表示全部）
python3 .claude/skills/xxljob-ops/scripts/xxljob.py jobs
python3 .claude/skills/xxljob-ops/scripts/xxljob.py jobs --group 2

# 某任务最近的执行日志（默认最近7天、20条）
python3 .claude/skills/xxljob-ops/scripts/xxljob.py logs --job 5
python3 .claude/skills/xxljob-ops/scripts/xxljob.py logs --job 5 --days 1 --n 50

# 手动触发一次（写操作！见硬约束）
python3 .claude/skills/xxljob-ops/scripts/xxljob.py trigger --job 5 --param "tradeNo=xxx"
```

脚本从当前目录 `devops-config.json` 读取 `xxljob` 段（adminUrl / username / password，模板见 `devops-config.example.json`）；可用环境变量 `DEVOPS_CONFIG` 指定路径。

## 硬约束

- `groups / jobs / logs` 是查询，免确认。
- **`trigger` 是写操作，会真实执行一次任务**（可能产生业务数据、发消息、调外部接口），执行前必须向用户说明影响并得到确认。
- 只连测试环境的 XXL-JOB 控制台。

## 已知注意事项

- 执行结果编码：200=成功，500=失败，0=运行中或未回调。
- 调度成功（triggerCode=200）不等于执行成功（handleCode），两个都要看。
- 控制台通常是内网地址，确保运行环境网络可达。
