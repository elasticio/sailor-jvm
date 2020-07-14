package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
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
    public void publish(Throwable e, AMQP.BasicProperties options, byte[] message) {

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

        if (message != null) {
            final byte[] errorInput = createErrorInput(options, message);
            payloadBuilder.add("errorInput", toString(errorInput));
        }

        final JsonObject payload = payloadBuilder.build();

        byte[] errorPayload = payload.toString().getBytes();

        messagePublisher.publish(this.routingKey, errorPayload, options);
    }

    private byte[] createErrorInput(final AMQP.BasicProperties originalMessageProperties, final byte[] message) {

        final MessageEncoding messageEncoding = Utils.getMessageEncoding(originalMessageProperties);

        if (messageEncoding == MessageEncoding.UTF8) {
            final String decrypted = cipher.decrypt(message, MessageEncoding.UTF8);

            return cipher.encrypt(decrypted, MessageEncoding.BASE64);
        }

        return message;
    }

    private String toString(byte[] bytes) {
        try {
            return new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
