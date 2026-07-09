package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.ClearingDtos.FeeCommission;
import com.wenhao.pay.agent.model.dto.ClearingDtos.OrderDiff;
import com.wenhao.pay.agent.model.dto.ClearingDtos.TxRecord;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 清算 / 对账查询工具（只读）。SQL 按占位表结构编写，接入真实库时调整字段映射。查询前已做表白名单校验。
 */
@Component
public class TxRecordQueryTool {

    private final ToolQueryGuard guard;
    private final JdbcTemplate jdbc;

    public TxRecordQueryTool(ToolQueryGuard guard, JdbcTemplate jdbc) {
        this.guard = guard;
        this.jdbc = jdbc;
    }

    @Tool(description = "查询清算交易记录")
    public TxRecord queryTxRecord(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("tx_record");
        List<TxRecord> rows = jdbc.query(
                "SELECT trade_no, amount, fee, state, settle_time FROM tx_record WHERE trade_no = ?",
                (rs, rowNum) -> new TxRecord(
                        rs.getString("trade_no"),
                        rs.getObject("amount", Long.class),
                        rs.getObject("fee", Long.class),
                        rs.getObject("state", Integer.class),
                        PayStates.txStateDesc(rs.getObject("state", Integer.class)),
                        rs.getObject("settle_time", LocalDateTime.class)),
                tradeNo);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到清算记录：" + tradeNo + "（可能漏单或清算未跑）");
        }
        return rows.get(0);
    }

    @Tool(description = "查询手续费与佣金明细")
    public FeeCommission queryFeeCommission(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("tx_record");
        List<FeeCommission> rows = jdbc.query(
                "SELECT trade_no, fee, commission, fee_rule FROM tx_record WHERE trade_no = ?",
                (rs, rowNum) -> new FeeCommission(
                        rs.getString("trade_no"),
                        rs.getObject("fee", Long.class),
                        rs.getObject("commission", Long.class),
                        rs.getString("fee_rule")),
                tradeNo);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到手续费记录：" + tradeNo);
        }
        return rows.get(0);
    }

    @Tool(description = "批量比对某时间段内平台与渠道订单，返回差异列表")
    public List<OrderDiff> batchCompareOrders(
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime) {
        guard.checkTableAllowed("pay_order");
        guard.checkTableAllowed("tx_record");
        guard.checkTimeRange(startTime, endTime);
        // 占位实现：以清算记录作为渠道侧对照，筛出无清算记录或金额不一致的订单；
        // 接入渠道对账文件后，把 tx_record 换成对账明细表即可。
        String sql = """
                SELECT p.trade_no, p.state AS platform_state, t.state AS channel_state,
                       p.amount AS platform_amount, t.amount AS channel_amount
                FROM pay_order p
                LEFT JOIN tx_record t ON p.trade_no = t.trade_no
                WHERE p.create_time BETWEEN ? AND ?
                  AND (t.trade_no IS NULL OR t.amount <> p.amount)
                LIMIT %d
                """.formatted(guard.logMaxResults());
        return jdbc.query(sql,
                (rs, rowNum) -> {
                    Long platformAmount = rs.getObject("platform_amount", Long.class);
                    Long channelAmount = rs.getObject("channel_amount", Long.class);
                    Integer channelState = rs.getObject("channel_state", Integer.class);
                    String diffType = channelState == null && channelAmount == null
                            ? "缺失记录(无清算记录)"
                            : Objects.equals(platformAmount, channelAmount) ? "状态差异" : "金额差异";
                    return new OrderDiff(
                            rs.getString("trade_no"),
                            PayStates.payStateDesc(rs.getObject("platform_state", Integer.class)),
                            PayStates.txStateDesc(channelState),
                            platformAmount,
                            channelAmount,
                            diffType);
                },
                startTime, endTime);
    }
}
