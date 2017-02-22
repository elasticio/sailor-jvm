package io.elastic.sailor.impl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.Message;
import io.elastic.api.Module;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ExecutionStats;
import io.elastic.sailor.MessageProcessor;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final CryptoServiceImpl cipher;
    private final MessageProcessor processor;
    private final Module module;

    public MessageConsumer(Channel channel, CryptoServiceImpl cipher, MessageProcessor processor, Module module) {
        super(channel);
        this.cipher = cipher;
        this.processor = processor;
        this.module = module;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        Message message;
        long deliveryTag = envelope.getDeliveryTag();

        final Object messageId = getHeaderValue(properties, Constants.AMQP_HEADER_MESSAGE_ID);
        final Object parentMessageId = getHeaderValue(properties, Constants.AMQP_HEADER_PARENT_MESSAGE_ID);
        final Object traceId = getHeaderValue(properties, Constants.AMQP_META_HEADER_TRACE_ID);

        logger.info("Consumer {} received message: deliveryTag={}, messageId={}, parentMessageId={}, traceId={}",
                consumerTag, deliveryTag, messageId, parentMessageId, traceId);

        try {
            // decrypt message
            String bodyString = new String(body, "UTF-8");
            message = cipher.decryptMessage(bodyString);
        } catch (Exception e) {
            logger.info("Failed to decrypt message {}", deliveryTag, e);
            this.getChannel().basicReject(deliveryTag, false);
            return;
        }

        ExecutionStats stats = null;

        try {
            stats = processor.processMessage(message, properties, this.module);
        } catch (Exception e) {
            logger.error("Failed to process message for delivery tag:" + deliveryTag, e);
        } finally {
            ackOrReject(stats, deliveryTag);
        }
    }

    private void ackOrReject(ExecutionStats stats, long deliveryTag) throws IOException {
        logger.info("Execution stats: {}", stats);

        if (stats == null || stats.getErrorCount() > 0) {
            logger.info("Reject received messages {}", deliveryTag);
            this.getChannel().basicReject(deliveryTag, false);

            return;
        }

        logger.info("Acknowledging received messages {}", deliveryTag);
        this.getChannel().basicAck(deliveryTag, true);
    }

    private Object getHeaderValue(final AMQP.BasicProperties properties, final String headerName) {
        return properties.getHeaders().getOrDefault(headerName, "unknown");
    }

}
