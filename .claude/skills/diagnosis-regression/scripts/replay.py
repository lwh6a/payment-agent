#!/usr/bin/env python3
"""诊断质量回归脚本：回放 badcase 用例，验证诊断结论不退化（只依赖 Python 标准库）。

用法：
  python3 replay.py                                  跑 cases/cases.json 全部用例
  python3 replay.py --case pay-001                   只跑指定用例
  python3 replay.py --base-url http://localhost:8088 指定服务地址

判定标准：/agent/diagnose 返回的诊断报告文本包含用例的全部 expect_keywords 即通过。
"""
import argparse
import json
import os
import sys
import urllib.request

CASES_PATH = os.path.join(os.path.dirname(__file__), "..", "cases", "cases.json")


def load_cases(case_id):
    with open(CASES_PATH, encoding="utf-8") as f:
        cases = json.load(f)
    if case_id:
        cases = [c for c in cases if c["id"] == case_id]
        if not cases:
            sys.exit(f"未找到用例：{case_id}")
    return cases


def run_case(base_url, case):
    payload = json.dumps({"question": case["question"]}).encode()
    req = urllib.request.Request(
        f"{base_url}/agent/diagnose",
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    with urllib.request.urlopen(req, timeout=180) as resp:
        report = resp.read().decode("utf-8", errors="replace")
    missing = [k for k in case["expect_keywords"] if k not in report]
    return missing, report


def main():
    parser = argparse.ArgumentParser(description="诊断质量回归")
    parser.add_argument("--case", default="", help="只跑指定用例 id")
    parser.add_argument("--base-url", default="http://localhost:8088")
    parser.add_argument("--verbose", action="store_true", help="失败时输出完整诊断报告")
    args = parser.parse_args()

    cases = load_cases(args.case)
    failed = 0
    for case in cases:
        try:
            missing, report = run_case(args.base_url, case)
        except Exception as e:
            failed += 1
            print(f"❌ {case['id']} [{case['type']}] 请求失败：{e}")
            continue
        if missing:
            failed += 1
            print(f"❌ {case['id']} [{case['type']}] 缺少关键词：{missing}")
            if args.verbose:
                print(f"--- 诊断报告 ---\n{report}\n")
        else:
            print(f"✅ {case['id']} [{case['type']}]")

    print(f"\n共 {len(cases)} 例：通过 {len(cases) - failed}，失败 {failed}")
    sys.exit(1 if failed else 0)


if __name__ == "__main__":
    main()
