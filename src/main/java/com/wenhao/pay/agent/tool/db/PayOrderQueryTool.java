package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.PayOrderDtos.CallbackLog;
import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayItemDiagnosis;
import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayOrderDiagnosis;
import com.wenhao.pay.agent.model.dto.PayOrderDtos.PayOrderSummary;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 支付单查询工具（只读）。SQL 按占位表结构编写，接入真实库时调整字段映射。查询前已做表白名单校验。
 */
@Component
public class PayOrderQueryTool {

    private static final String PAY_ORDER_COLS =
            "id, trade_no, order_no, order_type, user_id, amount_total, amount, real_amount, state, "
                    + "pay_mode, pay_mode_sub, create_time, paid_time, expired_time, is_split_account, channel_mchid, notify_url";

    private static final RowMapper<PayOrderDiagnosis> PAY_ORDER_MAPPER = (rs, rowNum) -> new PayOrderDiagnosis(
            rs.getObject("id", Long.class),
            rs.getString("trade_no"),
            rs.getString("order_no"),
            rs.getObject("order_type", Integer.class),
            rs.getString("user_id"),
            rs.getObject("amount_total", Long.class),
            rs.getObject("amount", Long.class),
            rs.getObject("real_amount", Long.class),
            rs.getObject("state", Integer.class),
            PayStates.payStateDesc(rs.getObject("state", Integer.class)),
            rs.getObject("pay_mode", Integer.class),
            PayStates.payModeDesc(rs.getObject("pay_mode", Integer.class)),
            rs.getObject("pay_mode_sub", Integer.class),
            rs.getObject("create_time", LocalDateTime.class),
            rs.getObject("paid_time", LocalDateTime.class),
            rs.getObject("expired_time", LocalDateTime.class),
            rs.getObject("is_split_account", Boolean.class),
            rs.getString("channel_mchid"),
            rs.getString("notify_url"));

    private static final RowMapper<PayItemDiagnosis> PAY_ITEM_MAPPER = (rs, rowNum) -> new PayItemDiagnosis(
            rs.getObject("id", Long.class),
            rs.getString("trade_no"),
            rs.getString("third_trade_no"),
            rs.getObject("state", Integer.class),
            PayStates.payStateDesc(rs.getObject("state", Integer.class)),
            rs.getObject("pay_mode", Integer.class),
            rs.getObject("amount", Long.class),
            rs.getObject("create_time", LocalDateTime.class));

    private final ToolQueryGuard guard;
    private final JdbcTemplate jdbc;

    public PayOrderQueryTool(ToolQueryGuard guard, JdbcTemplate jdbc) {
        this.guard = guard;
        this.jdbc = jdbc;
    }

    @Tool(description = "查询支付单基本信息和当前状态，入参交易号 tradeNo 或订单号 orderNo")
    public PayOrderDiagnosis queryPayOrder(
            @ToolParam(description = "交易号 tradeNo") String tradeNo,
            @ToolParam(required = false, description = "订单号 orderNo，可选") String orderNo) {
        guard.checkTableAllowed("pay_order");
        String column;
        String key;
        if (StringUtils.hasText(tradeNo)) {
            column = "trade_no";
            key = tradeNo;
        } else if (StringUtils.hasText(orderNo)) {
            column = "order_no";
            key = orderNo;
        } else {
            throw new IllegalArgumentException("tradeNo 与 orderNo 至少提供一个");
        }
        List<PayOrderDiagnosis> rows = jdbc.query(
                "SELECT " + PAY_ORDER_COLS + " FROM pay_order WHERE " + column + " = ?", PAY_ORDER_MAPPER, key);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到支付单：" + key);
        }
        return rows.get(0);
    }

    @Tool(description = "查询支付单关联的支付明细（含第三方交易号、渠道状态）")
    public List<PayItemDiagnosis> queryPayItem(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("pay_item");
        return jdbc.query(
                "SELECT id, trade_no, third_trade_no, state, pay_mode, amount, create_time"
                        + " FROM pay_item WHERE trade_no = ? ORDER BY id", PAY_ITEM_MAPPER, tradeNo);
    }

    @Tool(description = "查询支付回调日志，判断是否收到渠道回调及回调内容")
    public List<CallbackLog> queryPayCallbackLog(
            @ToolParam(description = "交易号 tradeNo") String tradeNo) {
        guard.checkTableAllowed("pay_callback_log");
        // 回调报文可能很大，截断到 500 字符，避免 Token 溢出
        return jdbc.query(
                "SELECT id, trade_no, channel_code, LEFT(raw_body, 500) AS raw_body, result, callback_time"
                        + " FROM pay_callback_log WHERE trade_no = ? ORDER BY callback_time DESC",
                (rs, rowNum) -> new CallbackLog(
                        rs.getObject("id", Long.class),
                        rs.getString("trade_no"),
                        rs.getString("channel_code"),
                        rs.getString("raw_body"),
                        rs.getObject("result", Integer.class),
                        rs.getObject("callback_time", LocalDateTime.class)),
                tradeNo);
    }

    @Tool(description = "按时间范围与状态批量查询支付单，用于排查某段时间卡单情况")
    public List<PayOrderSummary> queryPayOrderByTimeRange(
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime,
            @ToolParam(required = false, description = "支付状态码，可选") Integer state) {
        guard.checkTableAllowed("pay_order");
        guard.checkTimeRange(startTime, endTime);
        StringBuilder sql = new StringBuilder(
                "SELECT trade_no, state, amount, create_time FROM pay_order WHERE create_time BETWEEN ? AND ?");
        List<Object> args = new ArrayList<>(List.of(startTime, endTime));
        if (state != null) {
            sql.append(" AND state = ?");
            args.add(state);
        }
        sql.append(" ORDER BY create_time DESC LIMIT ").append(guard.logMaxResults());
        return jdbc.query(sql.toString(),
                (rs, rowNum) -> new PayOrderSummary(
                        rs.getString("trade_no"),
                        rs.getObject("state", Integer.class),
                        PayStates.payStateDesc(rs.getObject("state", Integer.class)),
                        rs.getObject("amount", Long.class),
                        rs.getObject("create_time", LocalDateTime.class)),
                args.toArray());
    }
}
