package com.wenhao.pay.agent.tool.db;

import com.wenhao.pay.agent.model.dto.LedgerDtos.ConfigRule;
import com.wenhao.pay.agent.model.dto.LedgerDtos.LedgerItem;
import com.wenhao.pay.agent.model.dto.LedgerDtos.LedgerOrder;
import com.wenhao.pay.agent.model.dto.LedgerDtos.MerchantConfig;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 分账查询工具。复用 forward 模块的分账/配置 Service（只读）。方法体待实现。查询前已做表白名单校验。
 */
@Component
public class LedgerQueryTool {

    private final ToolQueryGuard guard;

    public LedgerQueryTool(ToolQueryGuard guard) {
        this.guard = guard;
    }

    @Tool(description = "查询分账单及其状态（INIT/PROCESSING/SUCCESS/FAIL）")
    public LedgerOrder queryLedgerOrder(
            @ToolParam(description = "支付单号 payOrderNo") String payOrderNo,
            @ToolParam(required = false, description = "分账单号 ledgerNo，可选") String ledgerNo) {
        guard.checkTableAllowed("ledger_order");
        throw new UnsupportedOperationException("待实现：queryLedgerOrder");
    }

    @Tool(description = "查询分账明细，列出各接收方及分账金额")
    public List<LedgerItem> queryLedgerItems(
            @ToolParam(description = "分账单号 ledgerNo") String ledgerNo) {
        guard.checkTableAllowed("ledger_item");
        throw new UnsupportedOperationException("待实现：queryLedgerItems");
    }

    @Tool(description = "查询商户分账配置（是否开启分账、分账模式）")
    public MerchantConfig queryMerchantConfig(
            @ToolParam(description = "商户号 merchantCode") String merchantCode) {
        guard.checkTableAllowed("config_merchant");
        throw new UnsupportedOperationException("待实现：queryMerchantConfig");
    }

    @Tool(description = "查询商户分账规则配置")
    public List<ConfigRule> queryConfigRules(
            @ToolParam(description = "商户号 merchantCode") String merchantCode) {
        guard.checkTableAllowed("config_rule");
        throw new UnsupportedOperationException("待实现：queryConfigRules");
    }
}
