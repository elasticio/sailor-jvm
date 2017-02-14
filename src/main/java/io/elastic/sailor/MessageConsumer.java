package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.Component;
import io.elastic.api.Message;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final CipherWrapper cipher;
    private final MessageProcessor processor;
    private final Component component;

    public MessageConsumer(Channel channel, CipherWrapper cipher, MessageProcessor processor, Component component) {
        super(channel);
        this.cipher = cipher;
        this.processor = processor;
        this.component = component;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        Message message;
        long deliveryTag = envelope.getDeliveryTag();

        logger.info("Consumer {} received message {}", consumerTag, deliveryTag);

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
            stats = processor.processMessage(message, properties.getHeaders(), this.component);
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

}
