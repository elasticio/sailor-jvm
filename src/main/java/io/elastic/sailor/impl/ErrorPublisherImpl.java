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
import java.io.PrintWriter;
import java.io.StringWriter;

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
                .add("error", cipher.encryptJsonObject(error));

        if (originalMessage != null) {
            payloadBuilder.add("errorInput", cipher.encryptMessage(originalMessage));
        }

        final JsonObject payload = payloadBuilder.build();

        byte[] errorPayload = payload.toString().getBytes();

        messagePublisher.publish(this.routingKey, errorPayload, options);
    }
}
