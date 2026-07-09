package com.wenhao.pay.agent.tool.nacos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wenhao.pay.agent.config.AgentProperties;
import com.wenhao.pay.agent.model.dto.InfraDtos.NacosConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Nacos 配置查询工具（只读），对接 Nacos Open API（agent.tool.nacos）。
 * 商户配置、路由规则、开关类配置的变更是支付故障的高频根因，与"最近有没有发版"同级别的排查维度。
 */
@Component
public class NacosConfigQueryTool {

    /** 配置内容最大展示长度，避免 Token 溢出。 */
    private static final int MAX_CONTENT_LEN = 4000;

    private final AgentProperties.Tool.Nacos config;
    private final RestClient http = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public NacosConfigQueryTool(AgentProperties properties) {
        this.config = properties.tool().nacos();
    }

    @Tool(description = "查询 Nacos 配置中心指定配置的当前内容，用于排查配置变更/配置错误引起的故障")
    public NacosConfig queryNacosConfig(
            @ToolParam(description = "配置 dataId") String dataId,
            @ToolParam(required = false, description = "配置分组，默认 DEFAULT_GROUP") String group) {
        if (!StringUtils.hasText(config.serverAddr())) {
            throw new IllegalStateException("Nacos 未配置（agent.tool.nacos），无法查询配置");
        }
        String groupName = StringUtils.hasText(group) ? group : "DEFAULT_GROUP";
        StringBuilder url = new StringBuilder(config.serverAddr().replaceAll("/$", ""))
                .append("/nacos/v1/cs/configs?dataId=").append(encode(dataId))
                .append("&group=").append(encode(groupName));
        if (StringUtils.hasText(config.namespace())) {
            url.append("&tenant=").append(encode(config.namespace()));
        }
        if (StringUtils.hasText(config.username())) {
            url.append("&accessToken=").append(encode(login()));
        }
        try {
            String content = http.get().uri(URI.create(url.toString())).retrieve().body(String.class);
            if (content != null && content.length() > MAX_CONTENT_LEN) {
                content = content.substring(0, MAX_CONTENT_LEN) + "...(已截断)";
            }
            return new NacosConfig(dataId, groupName, content);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 404) {
                throw new IllegalStateException("Nacos 配置不存在：dataId=" + dataId + ", group=" + groupName);
            }
            throw new IllegalStateException("Nacos 配置查询失败：" + e.getMessage(), e);
        }
    }

    private String login() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("username", config.username());
        form.add("password", config.password());
        try {
            String body = http.post()
                    .uri(config.serverAddr().replaceAll("/$", "") + "/nacos/v1/auth/login")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(body == null ? "{}" : body).path("accessToken").asText();
        } catch (Exception e) {
            throw new IllegalStateException("Nacos 登录失败：" + e.getMessage(), e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
