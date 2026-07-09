package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayItemDiagnosis;
import com.wenhao.pay.agent.model.dto.RefundDtos.RefundAmountValidation;
import com.wenhao.pay.agent.model.dto.RefundDtos.RefundItemSummary;
import com.wenhao.pay.agent.model.dto.RefundDtos.RefundRecord;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 退款查询工具（只读）。SQL 按占位表结构编写，接入真实库时调整字段映射。查询前已做表白名单校验。
 */
@Component
public class RefundQueryTool {

    private static final RowMapper<RefundRecord> REFUND_MAPPER = (rs, rowNum) -> new RefundRecord(
            rs.getString("refund_trade_no"),
            rs.getString("trade_no"),
            rs.getObject("refund_amount", Long.class),
            rs.getObject("state", Integer.class),
            PayStates.payStateDesc(rs.getObject("state", Integer.class)),
            rs.getString("third_refund_no"),
            rs.getObject("create_time", LocalDateTime.class));

    private final ToolQueryGuard guard;
    private final JdbcTemplate jdbc;

    public RefundQueryTool(ToolQueryGuard guard, JdbcTemplate jdbc) {
        this.guard = guard;
        this.jdbc = jdbc;
    }

    @Tool(description = "查询退款记录基本信息与当前状态")
    public RefundRecord queryRefundRecord(
            @ToolParam(description = "原交易号 tradeNo") String tradeNo,
            @ToolParam(required = false, description = "退款交易号 refundTradeNo，可选") String refundTradeNo) {
        guard.checkTableAllowed("refund_record");
        String column;
        String key;
        if (StringUtils.hasText(refundTradeNo)) {
            column = "refund_trade_no";
            key = refundTradeNo;
        } else if (StringUtils.hasText(tradeNo)) {
            column = "trade_no";
            key = tradeNo;
        } else {
            throw new IllegalArgumentException("tradeNo 与 refundTradeNo 至少提供一个");
        }
        // 同一原单可能有多笔退款，未指定退款号时取最近一笔
        List<RefundRecord> rows = jdbc.query(
                "SELECT refund_trade_no, trade_no, refund_amount, state, third_refund_no, create_time"
                        + " FROM refund_record WHERE " + column + " = ? ORDER BY create_time DESC",
                REFUND_MAPPER, key);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到退款记录：" + key + "（说明退款未受理，检查申请参数）");
        }
        return rows.get(0);
    }

    @Tool(description = "查询退款对应的支付明细 pay_item（含渠道退款号、退款状态）")
    public PayItemDiagnosis queryRefundPayItem(
            @ToolParam(description = "退款交易号 refundTradeNo") String refundTradeNo) {
        guard.checkTableAllowed("pay_item");
        // 占位表结构：退款产生的 pay_item 以退款交易号记录在 trade_no 字段
        List<PayItemDiagnosis> rows = jdbc.query(
                "SELECT id, trade_no, third_trade_no, state, pay_mode, amount, create_time"
                        + " FROM pay_item WHERE trade_no = ? ORDER BY id",
                (rs, rowNum) -> new PayItemDiagnosis(
                        rs.getObject("id", Long.class),
                        rs.getString("trade_no"),
                        rs.getString("third_trade_no"),
                        rs.getObject("state", Integer.class),
                        PayStates.payStateDesc(rs.getObject("state", Integer.class)),
                        rs.getObject("pay_mode", Integer.class),
                        rs.getObject("amount", Long.class),
                        rs.getObject("create_time", LocalDateTime.class)),
                refundTradeNo);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到退款支付明细：" + refundTradeNo);
        }
        return rows.get(0);
    }

    @Tool(description = "校验退款金额：返回订单总额、已退金额、可退余额、退款历史，用于判断是否超退")
    public RefundAmountValidation validateRefundAmount(
            @ToolParam(description = "原交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("pay_order");
        guard.checkTableAllowed("refund_record");
        List<Long> amounts = jdbc.query("SELECT amount FROM pay_order WHERE trade_no = ?",
                (rs, rowNum) -> rs.getObject("amount", Long.class), tradeNo);
        if (amounts.isEmpty()) {
            throw new IllegalStateException("未查询到原支付单：" + tradeNo);
        }
        long orderAmount = amounts.get(0) == null ? 0 : amounts.get(0);

        List<RefundItemSummary> history = jdbc.query(
                "SELECT refund_trade_no, refund_amount, state, create_time"
                        + " FROM refund_record WHERE trade_no = ? ORDER BY create_time",
                (rs, rowNum) -> new RefundItemSummary(
                        rs.getString("refund_trade_no"),
                        rs.getObject("refund_amount", Long.class),
                        rs.getObject("state", Integer.class),
                        rs.getObject("create_time", LocalDateTime.class)),
                tradeNo);
        // 已退金额统计非 REFUND_FAIL(70) 的记录：退款中按占用额度计，防止并发超退
        long refunded = history.stream()
                .filter(item -> item.state() == null || item.state() != 70)
                .mapToLong(item -> item.amount() == null ? 0 : item.amount())
                .sum();
        return new RefundAmountValidation(tradeNo, orderAmount, refunded, orderAmount - refunded,
                history.size(), history);
    }
}
