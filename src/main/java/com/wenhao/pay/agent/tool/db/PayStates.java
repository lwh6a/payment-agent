package com.wenhao.pay.agent.tool.db;

/**
 * 状态码 / 支付方式段 → 可读描述。与 {@code SystemPrompts} 里的状态机保持一致。
 * 占位映射，接入真实系统时按实际枚举调整。
 */
final class PayStates {

    private PayStates() {
    }

    /** 支付与退款共用的状态码段。 */
    static String payStateDesc(Integer state) {
        if (state == null) {
            return "未知";
        }
        return switch (state) {
            case 10 -> "FROZEN(冻结/未发起支付)";
            case 20 -> "WAIT_PAYING(待支付)";
            case 35 -> "PAYING(支付中)";
            case 30 -> "PAID_SUCCESS(支付成功)";
            case 90 -> "PAID_PENDING(支付待确认)";
            case 65 -> "REFUNDING(退款中)";
            case 60 -> "REFUND_SUCCESS(退款成功)";
            case 70 -> "REFUND_FAIL(退款失败)";
            default -> "未知状态(" + state + ")";
        };
    }

    /** 分账状态（占位映射）。 */
    static String ledgerStateDesc(Integer state) {
        if (state == null) {
            return "未知";
        }
        return switch (state) {
            case 0 -> "INIT(待分账)";
            case 1 -> "PROCESSING(分账中)";
            case 2 -> "SUCCESS(分账成功)";
            case 3 -> "FAIL(分账失败)";
            default -> "未知状态(" + state + ")";
        };
    }

    /** 清算状态（占位映射）。 */
    static String txStateDesc(Integer state) {
        if (state == null) {
            return "未知";
        }
        return switch (state) {
            case 0 -> "PENDING(未清算)";
            case 1 -> "CLEARED(已清算)";
            default -> "未知状态(" + state + ")";
        };
    }

    /** 支付方式段 → 渠道名（示例段，按实际渠道配置调整）。 */
    static String payModeDesc(Integer payMode) {
        if (payMode == null) {
            return "未知";
        }
        int segment = payMode / 1000;
        if (segment >= 200 && segment < 210) {
            return "支付宝(" + payMode + ")";
        }
        if (segment >= 210 && segment < 220) {
            return "微信(" + payMode + ")";
        }
        if (segment >= 300 && segment < 350) {
            return "银联(" + payMode + ")";
        }
        return "渠道(" + payMode + ")";
    }
}
