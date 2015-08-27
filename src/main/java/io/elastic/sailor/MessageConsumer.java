package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.Message;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final CipherWrapper cipher;
    private final MessageProcessor processor;

    public MessageConsumer(Channel channel, CipherWrapper cipher, MessageProcessor processor) {
        super(channel);
        this.cipher = cipher;
        this.processor = processor;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        Message message;

        logger.info("Consumer {} received message {}", consumerTag, envelope.getDeliveryTag());

        try {
            // decrypt message
            String bodyString = new String(body, "UTF-8");
            message = cipher.decryptMessage(bodyString);
        } catch (Exception e) {
            logger.info("Failed to decrypt message {}: {}", envelope.getDeliveryTag(), e.getMessage());
            this.getChannel().basicReject(envelope.getDeliveryTag(), false);
            return;
        }

        try {
            processor.processMessage(message, properties.getHeaders(), envelope.getDeliveryTag());
        } catch (Exception e) {
            logger.info("Failed to process message {}: {}", envelope.getDeliveryTag(), e.getMessage());
            this.getChannel().basicReject(envelope.getDeliveryTag(), false);
        }
    }

}
