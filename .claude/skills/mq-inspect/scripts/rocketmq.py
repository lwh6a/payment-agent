#!/usr/bin/env python3
"""RocketMQ 控制台（rocketmq-console / rocketmq-dashboard）查询脚本，只依赖标准库。

用法：
  python3 rocketmq.py topics [--filter 关键字] [--all]     Topic 列表
  python3 rocketmq.py groups [--filter 关键字]             消费组列表（含堆积）
  python3 rocketmq.py stats --group G                      消费组详情
  python3 rocketmq.py search --topic T [--hours 3]         按时间段查消息
  python3 rocketmq.py key --topic T --key K                按业务 Key 查消息
  python3 rocketmq.py view --topic T --msg-id ID           单条消息详情 + 消费轨迹

配置来源：devops-config.json 的 rocketmq 段（consoleUrl / username / password）。
可用环境变量 DEVOPS_CONFIG 指定配置文件路径。

注意：不同版本控制台接口路径可能有差异，调不通时从浏览器 Network 面板确认后调整。
"""
import argparse
import http.cookiejar
import json
import os
import sys
import time
import urllib.parse
import urllib.request

CONFIG_PATH = os.environ.get("DEVOPS_CONFIG", "devops-config.json")


def load_mq_config():
    if not os.path.exists(CONFIG_PATH):
        sys.exit(f"未找到 {CONFIG_PATH}，请先复制 devops-config.example.json 为 devops-config.json 并填写")
    with open(CONFIG_PATH, encoding="utf-8") as f:
        cfg = json.load(f).get("rocketmq")
    if not cfg:
        sys.exit("devops-config.json 缺少 rocketmq 配置段")
    return cfg


def make_opener(cfg):
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
    )
    if cfg.get("username"):
        url = f"{cfg['consoleUrl'].rstrip('/')}/login/login"
        data = urllib.parse.urlencode(
            {"username": cfg["username"], "password": cfg["password"]}
        ).encode()
        body = json.loads(opener.open(url, data=data, timeout=30).read())
        if body.get("status") != 0:
            sys.exit(f"控制台登录失败：{body}")
    return opener


def get(opener, cfg, path, **params):
    url = f"{cfg['consoleUrl'].rstrip('/')}/{path}"
    if params:
        url += "?" + urllib.parse.urlencode(params)
    body = json.loads(opener.open(url, timeout=30).read())
    if body.get("status") != 0:
        sys.exit(f"接口返回异常：{body.get('errMsg') or body}")
    return body.get("data")


def ts_text(millis):
    return time.strftime("%m-%d %H:%M:%S", time.localtime(millis / 1000)) if millis else "-"


def show_topics(opener, cfg, keyword, show_all):
    data = get(opener, cfg, "topic/list.query")
    topics = data.get("topicList", []) if isinstance(data, dict) else data
    for t in sorted(topics):
        if not show_all and (t.startswith("%RETRY%") or t.startswith("%DLQ%")):
            continue
        if keyword and keyword.lower() not in t.lower():
            continue
        print(t)


def show_groups(opener, cfg, keyword):
    data = get(opener, cfg, "consumer/groupList.query")
    for g in data:
        name = g.get("group", "")
        if keyword and keyword.lower() not in name.lower():
            continue
        print(f"{name}  在线:{g.get('count', 0)}  TPS:{g.get('consumeTps', 0)}  "
              f"堆积:{g.get('diffTotal', 0)}")


def show_messages(messages):
    if not messages:
        print("未查到消息")
        return
    for m in messages:
        props = m.get("properties", {})
        print(f"[{ts_text(m.get('storeTimestamp'))}] msgId={m.get('msgId', '')}  "
              f"KEYS={props.get('KEYS', '-')}  TAGS={props.get('TAGS', '-')}")
    print(f"\n({len(messages)} 条)")


def main():
    parser = argparse.ArgumentParser(description="RocketMQ 控制台查询")
    sub = parser.add_subparsers(dest="action", required=True)
    topics = sub.add_parser("topics")
    topics.add_argument("--filter", default="")
    topics.add_argument("--all", action="store_true")
    groups = sub.add_parser("groups")
    groups.add_argument("--filter", default="")
    stats = sub.add_parser("stats")
    stats.add_argument("--group", required=True)
    search = sub.add_parser("search")
    search.add_argument("--topic", required=True)
    search.add_argument("--hours", type=int, default=3)
    key = sub.add_parser("key")
    key.add_argument("--topic", required=True)
    key.add_argument("--key", required=True)
    view = sub.add_parser("view")
    view.add_argument("--topic", required=True)
    view.add_argument("--msg-id", required=True)
    args = parser.parse_args()

    cfg = load_mq_config()
    opener = make_opener(cfg)

    if args.action == "topics":
        show_topics(opener, cfg, args.filter, args.all)
    elif args.action == "groups":
        show_groups(opener, cfg, args.filter)
    elif args.action == "stats":
        data = get(opener, cfg, "consumer/queryTopicByConsumer.query", consumerGroup=args.group)
        print(json.dumps(data, ensure_ascii=False, indent=2, default=str))
    elif args.action == "search":
        now = int(time.time() * 1000)
        data = get(opener, cfg, "message/queryMessageByTopic.query",
                   topic=args.topic, begin=now - args.hours * 3600 * 1000, end=now)
        show_messages(data)
    elif args.action == "key":
        data = get(opener, cfg, "message/queryMessageByTopicAndKey.query",
                   topic=args.topic, key=args.key)
        show_messages(data)
    elif args.action == "view":
        data = get(opener, cfg, "message/viewMessage.query",
                   topic=args.topic, msgId=args.msg_id)
        print(json.dumps(data, ensure_ascii=False, indent=2, default=str))


if __name__ == "__main__":
    main()
