#!/usr/bin/env python3
"""腾讯云 CLS 日志查询脚本。

用法：
  python3 cls_query.py                                   SIT 最近15分钟 error 日志
  python3 cls_query.py --query "orderNo-xxx" --minutes 60  按关键词查
  python3 cls_query.py --env prod --minutes 30            查 PROD
  python3 cls_query.py --raw                              只按 appName 过滤，不加 error 条件

依赖：pip install tencentcloud-sdk-python
配置来源：devops-config.json 的 cls 段。可用环境变量 DEVOPS_CONFIG 指定配置文件路径。
"""
import argparse
import json
import os
import sys
import time

from tencentcloud.common import credential
from tencentcloud.cls.v20201016 import cls_client, models

CONFIG_PATH = os.environ.get("DEVOPS_CONFIG", "devops-config.json")


def load_cls_config():
    if not os.path.exists(CONFIG_PATH):
        sys.exit(f"未找到 {CONFIG_PATH}，请先复制 devops-config.example.json 为 devops-config.json 并填写")
    with open(CONFIG_PATH, encoding="utf-8") as f:
        cfg = json.load(f).get("cls")
    if not cfg:
        sys.exit("devops-config.json 缺少 cls 配置段")
    return cfg


def build_query(cfg, args):
    app = cfg["appName"]
    if args.query:
        return f"{app} AND ({args.query})"
    if args.raw:
        return app
    return f"{app} AND (error OR exception OR fail)"


def main():
    parser = argparse.ArgumentParser(description="CLS 日志查询")
    parser.add_argument("--env", choices=["sit", "prod"], default="sit")
    parser.add_argument("--minutes", type=int, default=15, help="查询最近 N 分钟，默认15")
    parser.add_argument("--query", default="", help="额外关键词，如订单号、类名、traceId")
    parser.add_argument("--limit", type=int, default=50)
    parser.add_argument("--raw", action="store_true", help="不追加 error 过滤条件")
    args = parser.parse_args()

    cfg = load_cls_config()
    topic_key = "sitTopicId" if args.env == "sit" else "prodTopicId"
    topic_id = cfg.get(topic_key)
    if not topic_id:
        sys.exit(f"devops-config.json 的 cls 段缺少 {topic_key}")

    cred = credential.Credential(cfg["secretId"], cfg["secretKey"])
    client = cls_client.ClsClient(cred, cfg["region"])

    now = int(time.time() * 1000)
    req = models.SearchLogRequest()
    req.TopicId = topic_id
    req.From = now - args.minutes * 60 * 1000
    req.To = now
    req.Query = build_query(cfg, args)
    req.Limit = args.limit
    req.Sort = "desc"

    resp = client.SearchLog(req)
    data = json.loads(resp.to_json_string())
    results = data.get("Results", [])
    log_field = cfg.get("logField", "message")

    print(f"环境: {args.env.upper()} | 窗口: 最近{args.minutes}分钟 | 查询: {req.Query}")
    print(f"命中: {len(results)} 条\n")
    for r in results:
        log_json = json.loads(r.get("LogJson", "{}"))
        ts = time.strftime("%m-%d %H:%M:%S", time.localtime(r.get("Time", 0) / 1000))
        content = log_json.get(log_field) or str(log_json)[:400]
        print(f"[{ts}] {content[:400]}")


if __name__ == "__main__":
    main()
