package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MessageConsumer extends DefaultConsumer {

    private static Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private Sailor sailor;

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public MessageConsumer(Channel channel, Sailor sailor) {
        super(channel);
        this.sailor = sailor;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        // @TODO decrypt message here

        // @TODO pass decrypted message and headers to sailor for processing
        sailor.processMessage(consumerTag, envelope, properties, body);
    }

}
