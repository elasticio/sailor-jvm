package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import io.elastic.api.Message;

public class MessageConsumer extends DefaultConsumer {

    Sailor.Callback callback;

    public MessageConsumer(Channel channel, Sailor.Callback callback) {
        super(channel);
        this.callback = callback;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        Message message;
        Map<String,Object> headers;

        // @TODO decrypt body
        // @TODO build message with body and attachments
        // @TODO extract headers
        // @TODO pass everything to callback

       this.callback.receive(message, headers);
    }

}
