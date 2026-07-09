package com.wenhao.pay.agent.tool.deploy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenhao.pay.agent.config.AgentProperties;
import com.wenhao.pay.agent.model.dto.InfraDtos.DeployRecord;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * 发版记录查询工具（只读），对接 Jenkins JSON API（agent.tool.jenkins）。
 * "故障时间点附近是否有发版"是排障的关键维度——链路数据正常但突然出问题时优先比对发版时间。
 * 只接查询接口，不接触发构建。
 */
@Component
public class DeployRecordTool {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AgentProperties.Tool.Jenkins config;
    private final RestClient http = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeployRecordTool(AgentProperties properties) {
        this.config = properties.tool().jenkins();
    }

    @Tool(description = "查询最近的发版构建记录（构建号、结果、构建时间、耗时），用于判断故障时间点附近是否有发版")
    public List<DeployRecord> queryRecentDeployments(
            @ToolParam(required = false, description = "返回条数，默认10") Integer count) {
        if (!StringUtils.hasText(config.url())) {
            throw new IllegalStateException("Jenkins 未配置（agent.tool.jenkins），无法查询发版记录");
        }
        int limit = (count == null || count <= 0) ? 10 : count;
        // tree 参数含 []{} 特殊字符，手动编码后用 URI 直连，绕开 RestClient 的 URI 模板解析
        String tree = URLEncoder.encode(
                "builds[number,result,building,timestamp,duration]{0," + limit + "}", StandardCharsets.UTF_8);
        URI uri = URI.create(config.url().replaceAll("/$", "")
                + "/job/" + config.jobPath() + "/api/json?tree=" + tree);
        String auth = Base64.getEncoder().encodeToString(
                (config.username() + ":" + config.apiToken()).getBytes(StandardCharsets.UTF_8));
        try {
            String body = http.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + auth)
                    .retrieve()
                    .body(String.class);
            List<DeployRecord> records = new ArrayList<>();
            for (JsonNode build : objectMapper.readTree(body == null ? "{}" : body).path("builds")) {
                String result = build.path("building").asBoolean() ? "BUILDING" : build.path("result").asText("UNKNOWN");
                String buildTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(build.path("timestamp").asLong()), ZoneId.systemDefault()).format(TIME_FORMAT);
                records.add(new DeployRecord(
                        build.path("number").asInt(),
                        result,
                        buildTime,
                        build.path("duration").asLong() / 1000));
            }
            return records;
        } catch (Exception e) {
            throw new IllegalStateException("Jenkins 发版记录查询失败：" + e.getMessage(), e);
        }
    }
}
