package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.PayOrderDtos.CallbackLog;
import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayItemDiagnosis;
import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayOrderDiagnosis;
import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayOrderSummary;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支付单查询工具。复用 PayOrderService / PayItemService（只读）。
 * 各方法体待实现：注入对应 Service，查询后转为 DTO 返回。查询前已做表白名单校验。
 */
@Component
public class PayOrderQueryTool {

    private final ToolQueryGuard guard;

    public PayOrderQueryTool(ToolQueryGuard guard) {
        this.guard = guard;
    }

    @Tool(description = "查询支付单基本信息和当前状态，入参交易号 tradeNo 或订单号 orderNo")
    public PayOrderDiagnosis queryPayOrder(
            @ToolParam(description = "交易号 tradeNo") String tradeNo,
            @ToolParam(required = false, description = "订单号 orderNo，可选") String orderNo) {
        guard.checkTableAllowed("pay_order");
        throw new UnsupportedOperationException("待实现：queryPayOrder");
    }

    @Tool(description = "查询支付单关联的支付明细（含第三方交易号、渠道状态）")
    public List<PayItemDiagnosis> queryPayItem(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("pay_item");
        throw new UnsupportedOperationException("待实现：queryPayItem");
    }

    @Tool(description = "查询支付回调日志，判断是否收到渠道回调及回调内容")
    public List<CallbackLog> queryPayCallbackLog(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("pay_callback_log");
        throw new UnsupportedOperationException("待实现：queryPayCallbackLog");
    }

    @Tool(description = "按时间范围与状态批量查询支付单，用于排查某段时间卡单情况")
    public List<PayOrderSummary> queryPayOrderByTimeRange(
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime,
            @ToolParam(required = false, description = "支付状态码，可选") Integer state) {
        guard.checkTableAllowed("pay_order");
        guard.checkTimeRange(startTime, endTime);
        throw new UnsupportedOperationException("待实现：queryPayOrderByTimeRange");
    }
}
