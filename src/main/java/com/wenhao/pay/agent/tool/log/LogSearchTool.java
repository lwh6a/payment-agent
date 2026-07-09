package com.wenhao.pay.agent.tool.log;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencentcloudapi.cls.v20201016.ClsClient;
import com.tencentcloudapi.cls.v20201016.models.LogInfo;
import com.tencentcloudapi.cls.v20201016.models.SearchLogRequest;
import com.tencentcloudapi.cls.v20201016.models.SearchLogResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.wenhao.pay.agent.config.AgentProperties;
import com.wenhao.pay.agent.model.dto.InfraDtos.LogEntry;
import com.wenhao.pay.agent.tool.ToolQueryGuard;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 日志搜索工具，对接腾讯云 CLS（agent.tool.log.cls）。
 * 时间跨度已在入口校验；返回条数用 {@code guard.logMaxResults()} 限制，避免 Token 溢出。
 * 日志字段名按常见约定做了多别名兼容（message/msg、traceId/trace_id 等），接入实际日志格式后可精简。
 */
@Component
public class LogSearchTool {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ToolQueryGuard guard;
    private final AgentProperties.Tool.Log logConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public LogSearchTool(ToolQueryGuard guard, AgentProperties properties) {
        this.guard = guard;
        this.logConfig = properties.tool().log();
    }

    @Tool(description = "按 trace_id 搜索全链路日志，用于定位异常发生的具体节点")
    public List<LogEntry> searchByTraceId(
            @ToolParam(description = "链路追踪 ID traceId") String traceId) {
        // traceId 检索没有时间入参，取配置允许的最大时间窗口
        LocalDateTime end = LocalDateTime.now();
        return search("\"" + traceId + "\"", end.minusDays(logConfig.maxTimeRangeDays()), end);
    }

    @Tool(description = "按关键词在指定时间范围、指定服务内搜索日志")
    public List<LogEntry> searchByKeyword(
            @ToolParam(description = "关键词，如交易号") String keyword,
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime,
            @ToolParam(required = false, description = "服务名，可选") String service) {
        guard.checkTimeRange(startTime, endTime);
        String query = "\"" + keyword + "\"";
        if (StringUtils.hasText(service)) {
            query = "\"" + service + "\" AND " + query;
        }
        return search(query, startTime, endTime);
    }

    @Tool(description = "搜索指定服务在指定时间范围内的 ERROR 级别日志")
    public List<LogEntry> searchErrorLogs(
            @ToolParam(description = "服务名") String service,
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime) {
        guard.checkTimeRange(startTime, endTime);
        return search("\"" + service + "\" AND (error OR exception OR fail)", startTime, endTime);
    }

    private List<LogEntry> search(String query, LocalDateTime startTime, LocalDateTime endTime) {
        AgentProperties.Tool.Log.Cls cls = logConfig.cls();
        if (!StringUtils.hasText(cls.secretId())) {
            throw new IllegalStateException("CLS 日志服务未配置（agent.tool.log.cls），无法查询日志");
        }
        SearchLogRequest request = new SearchLogRequest();
        request.setTopicId(cls.topicId());
        request.setFrom(toMillis(startTime));
        request.setTo(toMillis(endTime));
        request.setQuery(query);
        request.setLimit((long) guard.logMaxResults());
        request.setSort("desc");
        try {
            ClsClient client = new ClsClient(new Credential(cls.secretId(), cls.secretKey()), cls.region());
            SearchLogResponse response = client.SearchLog(request);
            return Arrays.stream(response.getResults()).map(this::toLogEntry).toList();
        } catch (TencentCloudSDKException e) {
            throw new IllegalStateException("CLS 日志查询失败：" + e.getMessage(), e);
        }
    }

    private LogEntry toLogEntry(LogInfo info) {
        JsonNode log;
        try {
            log = objectMapper.readTree(info.getLogJson() == null ? "{}" : info.getLogJson());
        } catch (Exception e) {
            log = objectMapper.createObjectNode();
        }
        String time = info.getTime() == null ? null
                : LocalDateTime.ofInstant(Instant.ofEpochMilli(info.getTime()), ZoneId.systemDefault()).format(TIME_FORMAT);
        return new LogEntry(
                time,
                first(log, "level", "log_level"),
                first(log, "service", "app", "appName"),
                first(log, "traceId", "trace_id"),
                first(log, "spanId", "span_id"),
                first(log, "logger", "class"),
                first(log, "message", "msg", "content", "log"),
                first(log, "stackTrace", "stack_trace", "exception"));
    }

    private static String first(JsonNode log, String... fields) {
        for (String field : fields) {
            JsonNode node = log.get(field);
            if (node != null && !node.isNull()) {
                return node.asText();
            }
        }
        return null;
    }

    private static long toMillis(LocalDateTime time) {
        return time.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
