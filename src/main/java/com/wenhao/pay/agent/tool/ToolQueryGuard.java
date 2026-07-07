package com.wenhao.pay.agent.tool;

import com.wenhao.pay.agent.advisor.UnsafeRequestException;
import com.wenhao.pay.agent.config.AgentProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tool 层查询防护：消费 application.yml 的 agent.tool.* 安全配置，是排障只读底线的硬保障。
 *
 * - 启动时校验数据库必须只读（readonly=true），否则拒绝启动；
 * - DB Tool 查询前必须通过表白名单检查；
 * - 日志/批量查询限制时间跨度与返回条数，避免大范围扫描和 Token 溢出；
 * - 渠道查询统一超时。
 *
 * 在工具执行链路中抛出的异常会作为错误文本返回给模型，由模型向用户说明无法查询。
 */
@Component
public class ToolQueryGuard {

    private final AgentProperties.Tool config;

    public ToolQueryGuard(AgentProperties properties) {
        this.config = properties.tool();
        if (!config.db().readonly()) {
            throw new IllegalStateException("排障 Agent 的数据库访问必须只读，请保持 agent.tool.db.readonly=true");
        }
    }

    /** 校验表是否在排障白名单（agent.tool.db.allowed-tables）内，DB Tool 查询前调用。 */
    public void checkTableAllowed(String table) {
        if (!config.db().allowedTables().contains(table)) {
            throw new UnsafeRequestException("表不在排障白名单内，拒绝查询：" + table);
        }
    }

    /** 校验日志/批量查询的时间跨度不超过 agent.tool.log.max-time-range-days。 */
    public void checkTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null || endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("时间范围不合法：startTime=" + startTime + ", endTime=" + endTime);
        }
        int maxDays = config.log().maxTimeRangeDays();
        if (Duration.between(startTime, endTime).toDays() > maxDays) {
            throw new IllegalArgumentException("查询时间跨度超过上限 " + maxDays + " 天，请缩小范围");
        }
    }

    /** 单次日志查询最大返回条数，Tool 实现做 limit 时使用。 */
    public int logMaxResults() {
        return config.log().maxResults();
    }

    /** 渠道查询统一超时，渠道 Tool 实现时使用。 */
    public Duration channelTimeout() {
        return Duration.ofSeconds(config.channel().timeoutSeconds());
    }
}
