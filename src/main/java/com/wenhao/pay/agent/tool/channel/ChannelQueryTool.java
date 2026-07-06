package com.wenhao.pay.agent.tool.channel;

import com.wenhao.pay.agent.model.dto.ChannelDtos.ChannelLedger;
import com.wenhao.pay.agent.model.dto.ChannelDtos.ChannelOrder;
import com.wenhao.pay.agent.model.dto.ChannelDtos.ChannelRefund;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 渠道查询工具。复用 BasePaymentService 的查询类方法（只查不写）。
 * 按 payMode 段路由到对应渠道适配器：210xxx 微信 / 220xxx 美富宝 / 240-250xxx 宝付 等。方法体待实现。
 * 实现时统一使用 {@code ToolQueryGuard#channelTimeout()} 作为渠道请求超时。
 */
@Component
public class ChannelQueryTool {

    @Tool(description = "查询渠道侧订单真实状态，用于与平台支付状态比对")
    public ChannelOrder queryChannelOrderStatus(
            @ToolParam(description = "支付方式编码，如 210001=微信APP") Integer payMode,
            @ToolParam(description = "第三方交易号") String thirdTradeNo,
            @ToolParam(description = "渠道商户号") String channelMchid) {
        throw new UnsupportedOperationException("待实现：queryChannelOrderStatus");
    }

    @Tool(description = "查询渠道侧退款真实状态")
    public ChannelRefund queryChannelRefundStatus(
            @ToolParam(description = "支付方式编码") Integer payMode,
            @ToolParam(description = "第三方退款号") String thirdRefundNo,
            @ToolParam(description = "渠道商户号") String channelMchid) {
        throw new UnsupportedOperationException("待实现：queryChannelRefundStatus");
    }

    @Tool(description = "查询渠道侧分账真实状态")
    public ChannelLedger queryChannelLedgerStatus(
            @ToolParam(description = "支付方式编码") Integer payMode,
            @ToolParam(description = "分账单号 ledgerNo") String ledgerNo,
            @ToolParam(description = "渠道商户号") String channelMchid) {
        throw new UnsupportedOperationException("待实现：queryChannelLedgerStatus");
    }
}
