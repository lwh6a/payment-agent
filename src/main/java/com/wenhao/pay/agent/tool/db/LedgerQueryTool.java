package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.LedgerDtos.ConfigRule;
import com.wenhao.pay.agent.model.dto.LedgerDtos.LedgerItem;
import com.wenhao.pay.agent.model.dto.LedgerDtos.LedgerOrder;
import com.wenhao.pay.agent.model.dto.LedgerDtos.MerchantConfig;
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
 * 分账查询工具（只读）。SQL 按占位表结构编写，接入真实库时调整字段映射。查询前已做表白名单校验。
 */
@Component
public class LedgerQueryTool {

    private static final RowMapper<LedgerOrder> LEDGER_ORDER_MAPPER = (rs, rowNum) -> new LedgerOrder(
            rs.getString("ledger_no"),
            rs.getString("pay_order_no"),
            rs.getObject("state", Integer.class),
            PayStates.ledgerStateDesc(rs.getObject("state", Integer.class)),
            rs.getObject("total_amount", Long.class),
            rs.getObject("create_time", LocalDateTime.class));

    private final ToolQueryGuard guard;
    private final JdbcTemplate jdbc;

    public LedgerQueryTool(ToolQueryGuard guard, JdbcTemplate jdbc) {
        this.guard = guard;
        this.jdbc = jdbc;
    }

    @Tool(description = "查询分账单及其状态（INIT/PROCESSING/SUCCESS/FAIL）")
    public LedgerOrder queryLedgerOrder(
            @ToolParam(description = "支付单号 payOrderNo") String payOrderNo,
            @ToolParam(required = false, description = "分账单号 ledgerNo，可选") String ledgerNo) {
        guard.checkTableAllowed("ledger_order");
        String column;
        String key;
        if (StringUtils.hasText(ledgerNo)) {
            column = "ledger_no";
            key = ledgerNo;
        } else if (StringUtils.hasText(payOrderNo)) {
            column = "pay_order_no";
            key = payOrderNo;
        } else {
            throw new IllegalArgumentException("payOrderNo 与 ledgerNo 至少提供一个");
        }
        List<LedgerOrder> rows = jdbc.query(
                "SELECT ledger_no, pay_order_no, state, total_amount, create_time"
                        + " FROM ledger_order WHERE " + column + " = ? ORDER BY create_time DESC",
                LEDGER_ORDER_MAPPER, key);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到分账单：" + key + "（说明分账未发起，检查商户分账配置与分账任务）");
        }
        return rows.get(0);
    }

    @Tool(description = "查询分账明细，列出各接收方及分账金额")
    public List<LedgerItem> queryLedgerItems(
            @ToolParam(description = "分账单号 ledgerNo") String ledgerNo) {
        guard.checkTableAllowed("ledger_item");
        return jdbc.query(
                "SELECT ledger_no, receiver_merchant, amount, type FROM ledger_item WHERE ledger_no = ? ORDER BY id",
                (rs, rowNum) -> new LedgerItem(
                        rs.getString("ledger_no"),
                        rs.getString("receiver_merchant"),
                        rs.getObject("amount", Long.class),
                        rs.getString("type")),
                ledgerNo);
    }

    @Tool(description = "查询商户分账配置（是否开启分账、分账模式）")
    public MerchantConfig queryMerchantConfig(
            @ToolParam(description = "商户号 merchantCode") String merchantCode) {
        guard.checkTableAllowed("config_merchant");
        List<MerchantConfig> rows = jdbc.query(
                "SELECT merchant_code, split_enabled, split_mode, remark FROM config_merchant WHERE merchant_code = ?",
                (rs, rowNum) -> new MerchantConfig(
                        rs.getString("merchant_code"),
                        rs.getObject("split_enabled", Boolean.class),
                        rs.getString("split_mode"),
                        rs.getString("remark")),
                merchantCode);
        if (rows.isEmpty()) {
            throw new IllegalStateException("未查询到商户分账配置：" + merchantCode);
        }
        return rows.get(0);
    }

    @Tool(description = "查询商户分账规则配置")
    public List<ConfigRule> queryConfigRules(
            @ToolParam(description = "商户号 merchantCode") String merchantCode) {
        guard.checkTableAllowed("config_rule");
        return jdbc.query(
                "SELECT merchant_code, rule_code, expression, enabled FROM config_rule WHERE merchant_code = ?",
                (rs, rowNum) -> new ConfigRule(
                        rs.getString("merchant_code"),
                        rs.getString("rule_code"),
                        rs.getString("expression"),
                        rs.getObject("enabled", Boolean.class)),
                merchantCode);
    }
}
