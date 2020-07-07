package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ErrorPublisher;
import io.elastic.sailor.MessagePublisher;
import io.elastic.sailor.Utils;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.UnsupportedEncodingException;

public class ErrorPublisherImpl implements ErrorPublisher {

    private MessagePublisher messagePublisher;
    private CryptoServiceImpl cipher;
    private String routingKey;

    @Inject
    public ErrorPublisherImpl(MessagePublisher messagePublisher,
                              CryptoServiceImpl cipher,
                              @Named(Constants.ENV_VAR_ERROR_ROUTING_KEY) String routingKey) {
        this.messagePublisher = messagePublisher;
        this.cipher = cipher;
        this.routingKey = routingKey;
    }

    @Override
    public void publish(Throwable e, AMQP.BasicProperties options, Message originalMessage) {

        final String stackTrace = Utils.getStackTrace(e);

        final JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", e.getClass().getName())
                .add("stack", stackTrace);

        if (e.getMessage() != null) {
            builder.add("message", e.getMessage());
        }

        final JsonObject error = builder.build();

        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add("error", toString(cipher.encryptJsonObject(error, MessageEncoding.BASE64)));

        if (originalMessage != null) {
            payloadBuilder.add("errorInput", toString(cipher.encryptMessage(originalMessage, MessageEncoding.BASE64)));
        }

        final JsonObject payload = payloadBuilder.build();

        byte[] errorPayload = payload.toString().getBytes();

        messagePublisher.publish(this.routingKey, errorPayload, options);
    }

    private String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
