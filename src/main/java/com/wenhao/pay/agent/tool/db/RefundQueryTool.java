package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayItemDiagnosis;
import com.wenhao.pay.agent.model.dto.RefundDtos.RefundAmountValidation;
import com.wenhao.pay.agent.model.dto.RefundDtos.RefundRecord;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 退款查询工具。复用 RefundRecordService（只读）。方法体待实现。
 */
@Component
public class RefundQueryTool {

    @Tool(description = "查询退款记录基本信息与当前状态")
    public RefundRecord queryRefundRecord(
            @ToolParam(description = "原交易号 tradeNo") String tradeNo,
            @ToolParam(required = false, description = "退款交易号 refundTradeNo，可选") String refundTradeNo) {
        throw new UnsupportedOperationException("待实现：queryRefundRecord");
    }

    @Tool(description = "查询退款对应的支付明细 pay_item（含渠道退款号、退款状态）")
    public PayItemDiagnosis queryRefundPayItem(
            @ToolParam(description = "退款交易号 refundTradeNo") String refundTradeNo) {
        throw new UnsupportedOperationException("待实现：queryRefundPayItem");
    }

    @Tool(description = "校验退款金额：返回订单总额、已退金额、可退余额、退款历史，用于判断是否超退")
    public RefundAmountValidation validateRefundAmount(
            @ToolParam(description = "原交易号 tradeNo") String tradeNo) {
        throw new UnsupportedOperationException("待实现：validateRefundAmount");
    }
}
