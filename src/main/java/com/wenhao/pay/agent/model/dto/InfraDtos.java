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

    public record JobInfo(
            Integer id,
            String jobDesc,
            String executorHandler,
            String cron,
            Boolean enabled
    ) {
    }

    public record JobRunLog(
            Long logId,
            String triggerTime,
            String triggerResult,
            String handleResult,
            String handleTime
    ) {
    }

    public record DeployRecord(
            Integer buildNumber,
            String result,
            String buildTime,
            Long durationSeconds
    ) {
    }

    public record NacosConfig(
            String dataId,
            String group,
            String content
    ) {
    }
}
