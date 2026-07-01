package com.wenhao.pay.agent.model.dto;

import java.time.LocalDateTime;

/**
 * 渠道侧查询结果 DTO（占位）。用于与平台侧状态/金额比对。
 */
public final class ChannelDtos {

    private ChannelDtos() {
    }

    public record ChannelOrder(
            String thirdTradeNo,
            Integer payMode,
            String channelState,
            Long amount,
            LocalDateTime payTime
    ) {
    }

    public record ChannelRefund(
            String thirdRefundNo,
            Integer payMode,
            String channelState,
            Long amount,
            LocalDateTime refundTime
    ) {
    }

    public record ChannelLedger(
            String ledgerNo,
            Integer payMode,
            String channelState,
            Long amount
    ) {
    }
}
