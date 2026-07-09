#!/usr/bin/env python3
"""Jenkins 发版脚本（只依赖 Python 标准库）。

用法：
  python3 jenkins.py trigger      触发构建并轮询等待完成（最多10分钟）
  python3 jenkins.py status       查看最近一次构建状态
  python3 jenkins.py log [N]      输出最近一次构建日志尾部 N 行（默认100）

配置来源：devops-config.json 的 jenkins 段（url / username / apiToken / jobPath）。
可用环境变量 DEVOPS_CONFIG 指定配置文件路径。
"""
import base64
import json
import os
import sys
import time
import urllib.request

CONFIG_PATH = os.environ.get("DEVOPS_CONFIG", "devops-config.json")
POLL_INTERVAL = 30   # 轮询间隔（秒）
POLL_TIMEOUT = 600   # 轮询超时（秒）


def load_jenkins_config():
    if not os.path.exists(CONFIG_PATH):
        sys.exit(f"未找到 {CONFIG_PATH}，请先复制 devops-config.example.json 为 devops-config.json 并填写")
    with open(CONFIG_PATH, encoding="utf-8") as f:
        cfg = json.load(f).get("jenkins")
    if not cfg:
        sys.exit("devops-config.json 缺少 jenkins 配置段")
    return cfg


def request(cfg, path, method="GET", headers=None):
    url = f"{cfg['url'].rstrip('/')}/{path}"
    auth = base64.b64encode(f"{cfg['username']}:{cfg['apiToken']}".encode()).decode()
    all_headers = {"Authorization": f"Basic {auth}"}
    all_headers.update(headers or {})
    req = urllib.request.Request(url, method=method, headers=all_headers)
    with urllib.request.urlopen(req, timeout=30) as resp:
        return resp.read().decode("utf-8", errors="replace")


def get_last_build(cfg):
    return json.loads(request(cfg, f"job/{cfg['jobPath']}/lastBuild/api/json"))


def tail_log(cfg, n):
    text = request(cfg, f"job/{cfg['jobPath']}/lastBuild/consoleText")
    return "\n".join(text.splitlines()[-n:])


def trigger(cfg):
    headers = {}
    try:
        crumb = request(cfg, "crumbIssuer/api/xml?xpath=concat(//crumbRequestField,%22:%22,//crumb)")
        field, value = crumb.split(":", 1)
        headers[field] = value
    except Exception:
        pass  # API Token 认证下部分 Jenkins 版本不需要 crumb

    prev_number = get_last_build(cfg)["number"]
    request(cfg, f"job/{cfg['jobPath']}/buildWithParameters", method="POST", headers=headers)
    print(f"已触发构建（上一次构建 #{prev_number}），开始轮询…")

    deadline = time.time() + POLL_TIMEOUT
    while time.time() < deadline:
        time.sleep(POLL_INTERVAL)
        build = get_last_build(cfg)
        if build["number"] == prev_number or build["building"]:
            print(f"  构建中… 当前 #{build['number']}")
            continue
        seconds = build["duration"] // 1000
        print(f"构建 #{build['number']} 完成：{build['result']}（{seconds // 60}m{seconds % 60}s）")
        if build["result"] != "SUCCESS":
            print("--- 最后 100 行构建日志 ---")
            print(tail_log(cfg, 100))
            sys.exit(1)
        return
    sys.exit(f"轮询超时（{POLL_TIMEOUT}秒），请到 Jenkins 页面确认构建状态")


def status(cfg):
    build = get_last_build(cfg)
    state = "构建中" if build["building"] else build["result"]
    print(f"#{build['number']} {state}  {build.get('url', '')}")


def main():
    cfg = load_jenkins_config()
    action = sys.argv[1] if len(sys.argv) > 1 else "status"
    if action == "trigger":
        trigger(cfg)
    elif action == "status":
        status(cfg)
    elif action == "log":
        n = int(sys.argv[2]) if len(sys.argv) > 2 else 100
        print(tail_log(cfg, n))
    else:
        sys.exit(__doc__)


if __name__ == "__main__":
    main()
