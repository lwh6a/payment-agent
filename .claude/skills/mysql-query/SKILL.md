---
name: mysql-query
description: 只读查询测试环境 MySQL，用于排障时确认数据现场。触发词：查库、查数据、看表结构、这条订单在库里是什么状态。依赖 devops-config.json 的 mysql 段（只读账号）和 pymysql。
---

# MySQL 只读查询

## 什么时候用

- 数据类问题取证：确认某条业务数据在库里的真实状态、字段值
- 排查前先看表结构（`DESC 表名` / `SHOW CREATE TABLE 表名`）
- 验证修复效果：修复发版后确认数据已被正确写入/更新

## 使用方式

```bash
# 查数据（默认连配置中 databases 的第一个库）
python3 .claude/skills/mysql-query/scripts/query.py "SELECT * FROM pay_order WHERE trade_no = 'xxx'"

# 指定库
python3 .claude/skills/mysql-query/scripts/query.py --db pay_core "SHOW TABLES"

# 看表结构
python3 .claude/skills/mysql-query/scripts/query.py "DESC pay_order"
```

脚本从当前目录 `devops-config.json` 读取 `mysql` 段（模板见 `devops-config.example.json`）；可用环境变量 `DEVOPS_CONFIG` 指定路径。

## 前置条件

```bash
pip3 install --user -r .claude/skills/requirements.txt   # 或单独：pip3 install --user pymysql
```

**mysql 段必须配置只读账号**（找 DBA 申请专用的 `readonly` 账号，不要用个人开发账号）。权限在数据库层兜底，脚本校验只是第二道防线。

## 硬约束

- 只允许 `SELECT / SHOW / DESC / DESCRIBE / EXPLAIN`，脚本会拒绝其他语句和多语句。
- `SELECT` 未带 LIMIT 时自动追加 `LIMIT 100`。
- 查询结果含手机号、身份证、银行卡等敏感字段时，输出给用户前先脱敏（保留前3后4）。
- 只连测试环境。生产数据查询不走本 Skill。
