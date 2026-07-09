package com.wenhao.pay.agent.tool.job;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenhao.pay.agent.config.AgentProperties;
import com.wenhao.pay.agent.model.dto.InfraDtos.JobInfo;
import com.wenhao.pay.agent.model.dto.InfraDtos.JobRunLog;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * XXL-JOB 任务查询工具（只读），对接控制台接口（agent.tool.xxljob）。
 * 对账、退款重试、回调补偿等都由定时任务驱动，"任务跑没跑、执行结果如何"是排障关键维度。
 * 只接查询接口，不接 trigger 等写操作——排障 Agent 的只读底线。
 */
@Component
public class XxlJobQueryTool {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AgentProperties.Tool.XxlJob config;
    private final RestClient http = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public XxlJobQueryTool(AgentProperties properties) {
        this.config = properties.tool().xxljob();
    }

    @Tool(description = "按描述关键词查询定时任务列表（任务ID、描述、handler、cron、启停状态），用于先找到要排查的任务ID")
    public List<JobInfo> queryJobs(
            @ToolParam(required = false, description = "任务描述关键词，可选，如：对账、退款") String keyword) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("jobGroup", "0");
        form.add("triggerStatus", "-1");
        form.add("jobDesc", keyword == null ? "" : keyword);
        form.add("executorHandler", "");
        form.add("author", "");
        form.add("start", "0");
        form.add("length", "100");
        JsonNode body = postForm("jobinfo/pageList", form);
        List<JobInfo> jobs = new ArrayList<>();
        for (JsonNode job : body.path("data")) {
            jobs.add(new JobInfo(
                    job.path("id").asInt(),
                    job.path("jobDesc").asText(),
                    job.path("executorHandler").asText(),
                    job.path("scheduleConf").asText(),
                    job.path("triggerStatus").asInt() == 1));
        }
        return jobs;
    }

    @Tool(description = "查询某定时任务最近的执行日志。注意：调度成功不等于执行成功，triggerResult 和 handleResult 都要看")
    public List<JobRunLog> queryJobRunLogs(
            @ToolParam(description = "任务ID jobId") Integer jobId,
            @ToolParam(required = false, description = "查询最近几天，默认7天") Integer days) {
        int queryDays = (days == null || days <= 0) ? 7 : days;
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(queryDays);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("jobGroup", "0");
        form.add("jobId", String.valueOf(jobId));
        form.add("logStatus", "-1");
        form.add("filterTime", start.format(TIME_FORMAT) + " - " + end.format(TIME_FORMAT));
        form.add("start", "0");
        form.add("length", "20");
        JsonNode body = postForm("joblog/pageList", form);
        List<JobRunLog> logs = new ArrayList<>();
        for (JsonNode log : body.path("data")) {
            logs.add(new JobRunLog(
                    log.path("id").asLong(),
                    log.path("triggerTime").asText(),
                    codeText(log.path("triggerCode").asInt()),
                    codeText(log.path("handleCode").asInt()),
                    log.path("handleTime").asText("-")));
        }
        return logs;
    }

    private JsonNode postForm(String path, MultiValueMap<String, String> form) {
        try {
            String body = http.post()
                    .uri(config.adminUrl().replaceAll("/$", "") + "/" + path)
                    .header(HttpHeaders.COOKIE, loginCookie())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body);
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("XXL-JOB 查询失败：" + e.getMessage(), e);
        }
    }

    private String loginCookie() {
        if (!StringUtils.hasText(config.adminUrl())) {
            throw new IllegalStateException("XXL-JOB 未配置（agent.tool.xxljob），无法查询定时任务");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("userName", config.username());
        form.add("password", config.password());
        try {
            ResponseEntity<String> response = http.post()
                    .uri(config.adminUrl().replaceAll("/$", "") + "/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toEntity(String.class);
            if (objectMapper.readTree(response.getBody() == null ? "{}" : response.getBody())
                    .path("code").asInt() != 200) {
                throw new IllegalStateException("XXL-JOB 登录失败：" + response.getBody());
            }
            List<String> cookies = response.getHeaders().get(HttpHeaders.SET_COOKIE);
            if (cookies == null || cookies.isEmpty()) {
                throw new IllegalStateException("XXL-JOB 登录未返回 Cookie");
            }
            // Set-Cookie 只取「键=值」部分
            return cookies.stream().map(cookie -> cookie.split(";", 2)[0]).collect(Collectors.joining("; "));
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("XXL-JOB 登录失败：" + e.getMessage(), e);
        }
    }

    private static String codeText(int code) {
        return switch (code) {
            case 200 -> "成功";
            case 500 -> "失败";
            case 0 -> "运行中或未回调";
            default -> String.valueOf(code);
        };
    }
}
