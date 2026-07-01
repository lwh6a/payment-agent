package com.wenhao.pay.agent.model.dto;

import java.time.LocalDateTime;

/**
 * 支付单相关诊断 DTO（占位）。
 * 实际项目中字段按 pay_order / pay_item / pay_callback_log 表结构补充。
 * 金额单位统一为「分」，Tool 输出给模型时再转为「元」。
 */
public final class PayOrderDtos {

    private PayOrderDtos() {
    }

    public record PayOrderDiagnosis(
            Long id,
            String tradeNo,
            String orderNo,
            Integer orderType,
            String userId,
            Long amountTotal,
            Long amount,
            Long realAmount,
            Integer state,
            String stateDesc,
            Integer payMode,
            String payModeDesc,
            Integer payModeSub,
            LocalDateTime createTime,
            LocalDateTime paidTime,
            LocalDateTime expiredTime,
            Boolean isSplitAccount,
            String channelMchid,
            String notifyUrl
    ) {
    }

    public record PayItemDiagnosis(
            Long id,
            String tradeNo,
            String thirdTradeNo,
            Integer state,
            String stateDesc,
            Integer payMode,
            Long amount,
            LocalDateTime createTime
    ) {
    }

    public record CallbackLog(
            Long id,
            String tradeNo,
            String channelCode,
            String rawBody,
            Integer result,
            LocalDateTime callbackTime
    ) {
    }

    public record PayOrderSummary(
            String tradeNo,
            Integer state,
            String stateDesc,
            Long amount,
            LocalDateTime createTime
    ) {
    }
}
