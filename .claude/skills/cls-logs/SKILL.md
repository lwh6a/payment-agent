---
name: cls-logs
description: 查询腾讯云 CLS 日志，用于发版后验证和问题排查。触发词：查日志、查 SIT 日志、看下报错、验证发版结果、查异常栈。依赖 devops-config.json 的 cls 段和 tencentcloud-sdk-python。
---

# CLS 日志查询

## 什么时候用

- Bug 修复闭环（`bug-fix-workflow`）发版后的日志验证
- 排查取证：报错类问题先查异常栈
- 用户单独说"查 SIT 日志 error" / "查一下这个交易号的日志"

## 使用方式

```bash
# 默认：SIT 环境，最近15分钟，error/exception/fail 关键词
python3 .claude/skills/cls-logs/scripts/cls_query.py

# 按关键词查（交易号、类名、traceId 等）
python3 .claude/skills/cls-logs/scripts/cls_query.py --query "T2026070100001" --minutes 60

# 查 PROD（需配置 prodTopicId）
python3 .claude/skills/cls-logs/scripts/cls_query.py --env prod --minutes 30

# 只按 appName 过滤，不追加 error 条件（看全量日志流）
python3 .claude/skills/cls-logs/scripts/cls_query.py --raw --limit 20
```

脚本从当前目录 `devops-config.json` 读取 `cls` 段（模板见 `devops-config.example.json`）；可用环境变量 `DEVOPS_CONFIG` 指定路径。

## 前置条件

```bash
pip3 install --user -r .claude/skills/requirements.txt   # 或单独：pip3 install --user tencentcloud-sdk-python
```

## 已知注意事项

- 发版后等 30 秒再查（Pod 滚动需要时间）。
- 日志内容字段名（`cls.logField`）因项目日志格式而异，由使用者在配置中指定。
- 发版验证的结论标准：最近15分钟无新增 ERROR → 通过；有 ERROR → 输出详情并标注"需人工确认"，不要直接判定失败。
- `__TAG__` 中有 pod_name / image_name，可用于确认新版本已滚动完成。
