#!/usr/bin/env python3
"""测试环境 MySQL 只读查询脚本。

用法：
  python3 query.py "SELECT * FROM pay_order WHERE trade_no = 'xxx'"
  python3 query.py --db pay_core "SHOW TABLES"

依赖：pip install pymysql
配置来源：devops-config.json 的 mysql 段（务必使用只读账号）。
可用环境变量 DEVOPS_CONFIG 指定配置文件路径。
"""
import argparse
import json
import os
import sys

import pymysql

CONFIG_PATH = os.environ.get("DEVOPS_CONFIG", "devops-config.json")
ALLOWED_PREFIXES = ("select", "show", "desc", "describe", "explain")
DEFAULT_LIMIT = 100
MAX_CELL_WIDTH = 60


def load_mysql_config():
    if not os.path.exists(CONFIG_PATH):
        sys.exit(f"未找到 {CONFIG_PATH}，请先复制 devops-config.example.json 为 devops-config.json 并填写")
    with open(CONFIG_PATH, encoding="utf-8") as f:
        cfg = json.load(f).get("mysql")
    if not cfg:
        sys.exit("devops-config.json 缺少 mysql 配置段")
    return cfg


def validate_sql(sql):
    stripped = sql.strip().rstrip(";").strip()
    if not stripped:
        sys.exit("SQL 不能为空")
    if ";" in stripped:
        sys.exit("只允许单条语句，拒绝执行")
    first_word = stripped.split(None, 1)[0].lower()
    if first_word not in ALLOWED_PREFIXES:
        sys.exit(f"只允许只读语句（{' / '.join(ALLOWED_PREFIXES)}），拒绝执行: {first_word}")
    if first_word == "select" and " limit " not in f" {stripped.lower()} ":
        stripped += f" LIMIT {DEFAULT_LIMIT}"
    return stripped


def format_cell(value):
    text = "NULL" if value is None else str(value)
    if len(text) > MAX_CELL_WIDTH:
        text = text[:MAX_CELL_WIDTH - 3] + "..."
    return text


def print_table(columns, rows):
    table = [[format_cell(v) for v in row] for row in rows]
    widths = []
    for i, col in enumerate(columns):
        widths.append(max([len(col)] + [len(row[i]) for row in table]))
    header = " | ".join(col.ljust(widths[i]) for i, col in enumerate(columns))
    print(header)
    print("-+-".join("-" * w for w in widths))
    for row in table:
        print(" | ".join(cell.ljust(widths[i]) for i, cell in enumerate(row)))
    print(f"\n({len(rows)} 行)")


def main():
    parser = argparse.ArgumentParser(description="MySQL 只读查询")
    parser.add_argument("sql", help="要执行的只读 SQL")
    parser.add_argument("--db", default="", help="库名，默认取配置 databases 的第一个")
    args = parser.parse_args()

    cfg = load_mysql_config()
    sql = validate_sql(args.sql)
    database = args.db or cfg["databases"][0]

    conn = pymysql.connect(
        host=cfg["host"],
        port=cfg.get("port", 3306),
        user=cfg["user"],
        password=cfg["password"],
        database=database,
        charset="utf8mb4",
        read_timeout=30,
    )
    try:
        with conn.cursor() as cursor:
            cursor.execute(sql)
            columns = [d[0] for d in cursor.description]
            rows = cursor.fetchall()
    finally:
        conn.close()

    print(f"库: {database} | SQL: {sql}\n")
    print_table(columns, rows)


if __name__ == "__main__":
    main()
