package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import io.elastic.api.Message;
import org.slf4j.LoggerFactory;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);

    private Sailor.Callback callback;
    private CipherWrapper cipher;

    public MessageConsumer(Channel channel, CipherWrapper cipher, Sailor.Callback callback) {
        super(channel);
        this.cipher = cipher;
        this.callback = callback;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        logger.info(String.format("Message %s arrived", envelope.getDeliveryTag()));

        try {
            // decrypt message
            String bodyString = new String(body, "UTF-8");
            Message message = cipher.decryptMessage(bodyString);
            this.callback.receive(message, properties.getHeaders(), envelope.getDeliveryTag());
        } catch (Exception e) {
            logger.info(String.format("Failed to process message %s: %s", envelope.getDeliveryTag(), e.getMessage()));
            this.getChannel().basicReject(envelope.getDeliveryTag(), false);
        }


    }

}
