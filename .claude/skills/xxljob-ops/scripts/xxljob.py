#!/usr/bin/env python3
"""XXL-JOB 查询与触发脚本（只依赖 Python 标准库）。

用法：
  python3 xxljob.py groups                        执行器列表
  python3 xxljob.py jobs [--group 2]              任务列表（0=全部执行器）
  python3 xxljob.py logs --job 5 [--days 7] [--n 20]   某任务最近执行日志
  python3 xxljob.py trigger --job 5 [--param x]   手动触发一次（写操作，须先经用户确认）

配置来源：devops-config.json 的 xxljob 段（adminUrl / username / password）。
可用环境变量 DEVOPS_CONFIG 指定配置文件路径。
"""
import argparse
import http.cookiejar
import json
import os
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timedelta

CONFIG_PATH = os.environ.get("DEVOPS_CONFIG", "devops-config.json")


def load_xxljob_config():
    if not os.path.exists(CONFIG_PATH):
        sys.exit(f"未找到 {CONFIG_PATH}，请先复制 devops-config.example.json 为 devops-config.json 并填写")
    with open(CONFIG_PATH, encoding="utf-8") as f:
        cfg = json.load(f).get("xxljob")
    if not cfg:
        sys.exit("devops-config.json 缺少 xxljob 配置段")
    return cfg


def login(cfg):
    opener = urllib.request.build_opener(
        urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar())
    )
    body = post(opener, cfg, "login", {"userName": cfg["username"], "password": cfg["password"]})
    if body.get("code") != 200:
        sys.exit(f"XXL-JOB 登录失败：{body}")
    return opener


def post(opener, cfg, path, params):
    url = f"{cfg['adminUrl'].rstrip('/')}/{path}"
    data = urllib.parse.urlencode(params).encode()
    with opener.open(url, data=data, timeout=30) as resp:
        return json.loads(resp.read())


def code_text(code):
    return {200: "成功", 500: "失败", 0: "-"}.get(code, str(code))


def show_groups(opener, cfg):
    body = post(opener, cfg, "jobgroup/pageList", {"start": 0, "length": 100, "appname": "", "title": ""})
    for g in body.get("data", []):
        print(f"[{g['id']}] {g['appname']}  {g['title']}")


def show_jobs(opener, cfg, group):
    body = post(opener, cfg, "jobinfo/pageList", {
        "jobGroup": group, "triggerStatus": -1,
        "jobDesc": "", "executorHandler": "", "author": "",
        "start": 0, "length": 200,
    })
    for j in body.get("data", []):
        status = "启用" if j.get("triggerStatus") == 1 else "停用"
        print(f"[{j['id']}] {j['jobDesc']}  handler={j.get('executorHandler', '')}  "
              f"cron={j.get('scheduleConf', '')}  {status}")


def show_logs(opener, cfg, job_id, days, count):
    end = datetime.now()
    start = end - timedelta(days=days)
    body = post(opener, cfg, "joblog/pageList", {
        "jobGroup": 0, "jobId": job_id, "logStatus": -1,
        "filterTime": f"{start:%Y-%m-%d %H:%M:%S} - {end:%Y-%m-%d %H:%M:%S}",
        "start": 0, "length": count,
    })
    logs = body.get("data", [])
    if not logs:
        print(f"任务 {job_id} 最近 {days} 天无执行记录")
        return
    for log in logs:
        print(f"[logId {log['id']}] 调度 {log.get('triggerTime', '')}  "
              f"调度结果:{code_text(log.get('triggerCode'))}  "
              f"执行结果:{code_text(log.get('handleCode'))}  "
              f"执行完成时间:{log.get('handleTime') or '-'}")


def trigger_job(opener, cfg, job_id, param):
    body = post(opener, cfg, "jobinfo/trigger", {"id": job_id, "executorParam": param, "addressList": ""})
    if body.get("code") == 200:
        print(f"已触发任务 {job_id}，稍后用 logs --job {job_id} 查看执行结果")
    else:
        sys.exit(f"触发失败：{body}")


def main():
    parser = argparse.ArgumentParser(description="XXL-JOB 查询与触发")
    sub = parser.add_subparsers(dest="action", required=True)
    sub.add_parser("groups")
    jobs = sub.add_parser("jobs")
    jobs.add_argument("--group", type=int, default=0)
    logs = sub.add_parser("logs")
    logs.add_argument("--job", type=int, required=True)
    logs.add_argument("--days", type=int, default=7)
    logs.add_argument("--n", type=int, default=20)
    trigger = sub.add_parser("trigger")
    trigger.add_argument("--job", type=int, required=True)
    trigger.add_argument("--param", default="")
    args = parser.parse_args()

    cfg = load_xxljob_config()
    opener = login(cfg)

    if args.action == "groups":
        show_groups(opener, cfg)
    elif args.action == "jobs":
        show_jobs(opener, cfg, args.group)
    elif args.action == "logs":
        show_logs(opener, cfg, args.job, args.days, args.n)
    elif args.action == "trigger":
        trigger_job(opener, cfg, args.job, args.param)


if __name__ == "__main__":
    main()
