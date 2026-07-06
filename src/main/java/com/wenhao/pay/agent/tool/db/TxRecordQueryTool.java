package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.ClearingDtos.OrderDiff;
import com.wenhao.pay.agent.model.dto.ClearingDtos.SettlementCommission;
import com.wenhao.pay.agent.model.dto.ClearingDtos.TxRecord;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 清算 / 对账查询工具。复用 clearing 模块 Service（只读）。方法体待实现。查询前已做表白名单校验。
 */
@Component
public class TxRecordQueryTool {

    private final ToolQueryGuard guard;

    public TxRecordQueryTool(ToolQueryGuard guard) {
        this.guard = guard;
    }

    @Tool(description = "查询清算交易记录")
    public TxRecord queryTxRecord(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("tx_record");
        throw new UnsupportedOperationException("待实现：queryTxRecord");
    }

    @Tool(description = "查询手续费与佣金明细")
    public SettlementCommission querySettlementCommission(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("tx_record");
        throw new UnsupportedOperationException("待实现：querySettlementCommission");
    }

    @Tool(description = "批量比对某时间段内平台与渠道订单，返回差异列表")
    public List<OrderDiff> batchCompareOrders(
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime) {
        guard.checkTableAllowed("pay_order");
        guard.checkTimeRange(startTime, endTime);
        throw new UnsupportedOperationException("待实现：batchCompareOrders");
    }
}
