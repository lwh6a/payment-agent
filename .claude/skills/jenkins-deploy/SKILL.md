---
name: jenkins-deploy
description: 触发 Jenkins 构建发版、轮询构建状态、拉取构建日志。触发词：发版、部署、构建 SIT、查构建状态、看构建日志。依赖 devops-config.json 的 jenkins 段，脚本只用 Python 标准库。
---

# Jenkins 发版

## 什么时候用

- Bug 修复闭环（`bug-fix-workflow`）的发版环节
- 用户单独说"发版 SIT" / "查一下构建状态" / "构建失败了看下日志"

## 使用方式

```bash
# 触发构建并轮询等待完成（每30秒查一次，最多10分钟；失败时自动带出最后100行日志）
python3 .claude/skills/jenkins-deploy/scripts/jenkins.py trigger

# 只查最近一次构建状态
python3 .claude/skills/jenkins-deploy/scripts/jenkins.py status

# 拉取最近一次构建日志尾部（默认100行）
python3 .claude/skills/jenkins-deploy/scripts/jenkins.py log 200
```

脚本从当前目录 `devops-config.json` 读取 `jenkins` 段（url / username / apiToken / jobPath，模板见 `devops-config.example.json`）。
配置文件不在当前目录时，用环境变量指定：`DEVOPS_CONFIG=/path/to/devops-config.json python3 ... status`。

## 硬约束

- **`trigger` 是写操作，会实际发版**。仅在用户明确要求发版、或走 Bug 修复闭环流程时执行；日常排查只用 `status` / `log`。
- 发版前先按 `deploy-checklist` 生成检查单，确认环境就绪。
- 发版失败时停止后续流程（不查日志验证、不回写工单状态），先输出失败原因。
- Jenkins 通常是内网地址，确保运行环境网络可达。

## 已知注意事项

- Pipeline Job 必须用 `buildWithParameters`，脚本已内置。
- CSRF crumb 脚本会自动获取；API Token 认证下部分 Jenkins 版本不需要 crumb，获取失败会自动忽略。
- 构建是异步的，`trigger` 会等到构建真正结束才返回结果。
