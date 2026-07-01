package com.wenhao.pay.agent.tool.log;

import com.wenhao.pay.agent.model.dto.InfraDtos.LogEntry;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志搜索工具。对接实际日志平台（CLS / ELK）。
 * 注意：返回条数需限制（默认 100），避免 Token 溢出。方法体待实现。
 */
@Component
public class LogSearchTool {

    @Tool(description = "按 trace_id 搜索全链路日志，用于定位异常发生的具体节点")
    public List<LogEntry> searchByTraceId(
            @ToolParam(description = "链路追踪 ID traceId") String traceId) {
        throw new UnsupportedOperationException("待实现：searchByTraceId");
    }

    @Tool(description = "按关键词在指定时间范围、指定服务内搜索日志")
    public List<LogEntry> searchByKeyword(
            @ToolParam(description = "关键词，如交易号") String keyword,
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime,
            @ToolParam(required = false, description = "服务名，可选") String service) {
        throw new UnsupportedOperationException("待实现：searchByKeyword");
    }

    @Tool(description = "搜索指定服务在指定时间范围内的 ERROR 级别日志")
    public List<LogEntry> searchErrorLogs(
            @ToolParam(description = "服务名") String service,
            @ToolParam(description = "开始时间") LocalDateTime startTime,
            @ToolParam(description = "结束时间") LocalDateTime endTime) {
        throw new UnsupportedOperationException("待实现：searchErrorLogs");
    }
}
