package com.wenhao.pay.agent.model.dto;

import java.time.LocalDateTime;

/**
 * 清算 / 对账相关诊断 DTO（占位）。
 */
public final class ClearingDtos {

    private ClearingDtos() {
    }

    public record TxRecord(
            String tradeNo,
            Long amount,
            Long fee,
            Integer state,
            String stateDesc,
            LocalDateTime settleTime
    ) {
    }

    public record FeeCommission(
            String tradeNo,
            Long fee,
            Long commission,
            String feeRule
    ) {
    }

    public record OrderDiff(
            String tradeNo,
            String platformState,
            String channelState,
            Long platformAmount,
            Long channelAmount,
            String diffType
    ) {
    }
}
