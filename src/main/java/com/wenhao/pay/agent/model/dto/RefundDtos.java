package com.wenhao.pay.agent.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款相关诊断 DTO（占位）。
 */
public final class RefundDtos {

    private RefundDtos() {
    }

    public record RefundRecord(
            String refundTradeNo,
            String tradeNo,
            Long refundAmount,
            Integer state,
            String stateDesc,
            String thirdRefundNo,
            LocalDateTime createTime
    ) {
    }

    public record RefundAmountValidation(
            String tradeNo,
            Long orderAmount,
            Long refundedAmount,
            Long refundableAmount,
            Integer refundedCount,
            List<RefundItemSummary> refundHistory
    ) {
    }

    public record RefundItemSummary(
            String refundTradeNo,
            Long amount,
            Integer state,
            LocalDateTime time
    ) {
    }
}
