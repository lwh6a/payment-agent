package com.wenhao.pay.agent.tool.mq;

import com.wenhao.pay.agent.config.AgentProperties;
import com.wenhao.pay.agent.model.dto.InfraDtos.ConsumerLag;
import com.wenhao.pay.agent.model.dto.InfraDtos.MqMessage;
import com.wenhao.pay.agent.model.dto.InfraDtos.MqTrace;
import jakarta.annotation.PreDestroy;
import org.apache.rocketmq.client.QueryResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.protocol.admin.ConsumeStats;
import org.apache.rocketmq.tools.admin.DefaultMQAdminExt;
import org.apache.rocketmq.tools.admin.api.MessageTrack;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * RocketMQ 状态查询工具（只读）。通过 Admin API 查询消息投递、消费轨迹与消费堆积。
 * Admin 客户端在首次查询时才连接 NameServer（agent.tool.mq.name-server）。
 * 消费轨迹依赖 broker 开启 trace（traceTopicEnable）。
 */
@Component
public class RocketMQStatusTool {

    /** 按 key 反查消息的时间窗口：最近 3 天。 */
    private static final long QUERY_WINDOW_MILLIS = Duration.ofDays(3).toMillis();
    /** 消息体最大展示长度，避免 Token 溢出。 */
    private static final int MAX_BODY_LEN = 2000;

    private final String nameServer;
    private DefaultMQAdminExt admin;

    public RocketMQStatusTool(AgentProperties properties) {
        this.nameServer = properties.tool().mq().nameServer();
    }

    @Tool(description = "按消息 key（一般为交易号）查询消息是否已投递、内容是什么，查询窗口为最近3天")
    public MqMessage queryMessageByKey(
            @ToolParam(description = "Topic 名") String topic,
            @ToolParam(description = "消息 key，一般为交易号") String messageKey) {
        long end = System.currentTimeMillis();
        QueryResult result;
        try {
            result = admin().queryMessage(topic, messageKey, 16, end - QUERY_WINDOW_MILLIS, end);
        } catch (Exception e) {
            throw new IllegalStateException("MQ 消息查询失败：" + e.getMessage(), e);
        }
        MessageExt latest = result.getMessageList().stream()
                .max(Comparator.comparingLong(MessageExt::getStoreTimestamp))
                .orElseThrow(() -> new IllegalStateException(
                        "按 key 未查询到消息（最近3天）：topic=" + topic + ", key=" + messageKey
                                + "。大概率生产端未发出，建议查生产端日志"));
        String body = latest.getBody() == null ? null : new String(latest.getBody(), StandardCharsets.UTF_8);
        if (body != null && body.length() > MAX_BODY_LEN) {
            body = body.substring(0, MAX_BODY_LEN) + "...(已截断)";
        }
        // 查得到即已投递，status 固定 1；消费情况用 queryMessageTrace 看轨迹
        return new MqMessage(topic, messageKey, latest.getMsgId(), body, 1,
                LocalDateTime.ofInstant(Instant.ofEpochMilli(latest.getStoreTimestamp()), ZoneId.systemDefault()));
    }

    @Tool(description = "按 msgId 查询消息的各消费组消费轨迹（CONSUMED已消费/NOT_CONSUME_YET未消费/FAILED消费失败等）")
    public MqTrace queryMessageTrace(
            @ToolParam(description = "Topic 名") String topic,
            @ToolParam(description = "消息 ID msgId") String msgId) {
        try {
            MessageExt message = admin().viewMessage(topic, msgId);
            List<MessageTrack> tracks = admin().messageTrackDetail(message);
            if (tracks.isEmpty()) {
                return new MqTrace(msgId, "NO_CONSUMER", "没有订阅该 Topic 的消费组");
            }
            String status = tracks.stream()
                    .map(track -> track.getTrackType().name())
                    .distinct()
                    .collect(Collectors.joining(","));
            String detail = tracks.stream()
                    .map(track -> "消费组=" + track.getConsumerGroup() + " 状态=" + track.getTrackType()
                            + (StringUtils.hasText(track.getExceptionDesc()) ? " 异常=" + track.getExceptionDesc() : ""))
                    .collect(Collectors.joining("\n"));
            return new MqTrace(msgId, status, detail);
        } catch (Exception e) {
            throw new IllegalStateException("MQ 消息轨迹查询失败：" + e.getMessage(), e);
        }
    }

    @Tool(description = "查询消费者组在某 Topic 的消费堆积量，判断是否积压")
    public ConsumerLag queryConsumerLag(
            @ToolParam(description = "消费者组") String consumerGroup,
            @ToolParam(description = "Topic 名") String topic) {
        try {
            ConsumeStats stats = admin().examineConsumeStats(consumerGroup, topic);
            return new ConsumerLag(consumerGroup, topic, stats.computeTotalDiff());
        } catch (Exception e) {
            throw new IllegalStateException("MQ 消费堆积查询失败：" + e.getMessage(), e);
        }
    }

    private synchronized DefaultMQAdminExt admin() {
        if (admin == null) {
            DefaultMQAdminExt ext = new DefaultMQAdminExt(5000);
            ext.setNamesrvAddr(nameServer);
            ext.setInstanceName("payment-agent-admin");
            try {
                ext.start();
            } catch (Exception e) {
                throw new IllegalStateException("RocketMQ Admin 启动失败（name-server=" + nameServer + "）：" + e.getMessage(), e);
            }
            admin = ext;
        }
        return admin;
    }

    @PreDestroy
    public void shutdown() {
        if (admin != null) {
            admin.shutdown();
        }
    }
}
