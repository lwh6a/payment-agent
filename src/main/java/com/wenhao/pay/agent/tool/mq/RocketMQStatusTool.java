package com.wenhao.pay.agent.tool.mq;

import com.wenhao.pay.agent.model.dto.InfraDtos.ConsumerLag;
import com.wenhao.pay.agent.model.dto.InfraDtos.MqMessage;
import com.wenhao.pay.agent.model.dto.InfraDtos.MqTrace;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 状态查询工具。通过 RocketMQ Admin API 查询消息投递与消费堆积。方法体待实现。
 * 关键 Topic：pay_callback_topic / withdraw_callback_topic / accountsplit_callback_topic 等。
 */
@Component
public class RocketMQStatusTool {

    @Tool(description = "按消息 key 查询消息是否已投递、内容是什么")
    public MqMessage queryMessageByKey(
            @ToolParam(description = "Topic 名") String topic,
            @ToolParam(description = "消息 key，一般为交易号") String messageKey) {
        throw new UnsupportedOperationException("待实现：queryMessageByKey");
    }

    @Tool(description = "按 msgId 查询消息轨迹（投递/消费明细）")
    public MqTrace queryMessageTrace(
            @ToolParam(description = "消息 ID msgId") String msgId) {
        throw new UnsupportedOperationException("待实现：queryMessageTrace");
    }

    @Tool(description = "查询消费者组在某 Topic 的消费堆积量，判断是否积压")
    public ConsumerLag queryConsumerLag(
            @ToolParam(description = "消费者组") String consumerGroup,
            @ToolParam(description = "Topic 名") String topic) {
        throw new UnsupportedOperationException("待实现：queryConsumerLag");
    }
}
