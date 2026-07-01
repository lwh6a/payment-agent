package com.wenhao.pay.agent.model.dto;

import java.time.LocalDateTime;

/**
 * 分账相关诊断 DTO（占位）。
 */
public final class LedgerDtos {

    private LedgerDtos() {
    }

    public record LedgerOrder(
            String ledgerNo,
            String payOrderNo,
            Integer state,
            String stateDesc,
            Long totalAmount,
            LocalDateTime createTime
    ) {
    }

    public record LedgerItem(
            String ledgerNo,
            String receiverMerchant,
            Long amount,
            String type
    ) {
    }

    public record MerchantConfig(
            String merchantCode,
            Boolean splitEnabled,
            String splitMode,
            String remark
    ) {
    }

    public record ConfigRule(
            String merchantCode,
            String ruleCode,
            String expression,
            Boolean enabled
    ) {
    }
}
