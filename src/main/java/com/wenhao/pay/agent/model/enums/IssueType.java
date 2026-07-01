package com.wenhao.pay.agent.model.enums;

/**
 * 排障问题类型，由 Orchestrator 意图识别后产出，用于路由到对应领域 Agent。
 */
public enum IssueType {

    PAYMENT,         // 支付异常
    REFUND,          // 退款异常
    LEDGER,          // 分账异常
    RECONCILIATION,  // 对账差异
    UNKNOWN          // 无法识别，交由人工
}
