package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.Message;

import java.io.IOException;

public class MessageConsumer extends DefaultConsumer {

    Sailor.Callback callback;
    CipherWrapper cipher;

    public MessageConsumer(Channel channel, Sailor.Callback callback) {
        super(channel);
        this.callback = callback;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        CipherWrapper cipher = new CipherWrapper("iv=any16_symbols".getBytes());

        // decrypt body
        String bodyString = new String(body, "UTF-8");
        JsonObject payload = cipher.decryptMessageContent(bodyString);
        Message message = new Message.Builder().body(payload).build();

        System.out.println(message.toString());
        this.callback.receive(message, properties.getHeaders(), envelope.getDeliveryTag());
    }

}