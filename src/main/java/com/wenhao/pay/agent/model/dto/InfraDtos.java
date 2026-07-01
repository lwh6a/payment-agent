package com.wenhao.pay.agent.model.dto;

import java.time.LocalDateTime;

/**
 * 日志 / MQ 等基础设施查询结果 DTO（占位）。
 */
public final class InfraDtos {

    private InfraDtos() {
    }

    public record LogEntry(
            String timestamp,
            String level,
            String service,
            String traceId,
            String spanId,
            String logger,
            String message,
            String stackTrace
    ) {
    }

    public record MqMessage(
            String topic,
            String messageKey,
            String msgId,
            String body,
            Integer status,
            LocalDateTime storeTime
    ) {
    }

    public record MqTrace(
            String msgId,
            String status,
            String traceDetail
    ) {
    }

    public record ConsumerLag(
            String consumerGroup,
            String topic,
            Long lag
    ) {
    }
}
