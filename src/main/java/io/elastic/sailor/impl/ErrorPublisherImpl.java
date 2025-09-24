package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ErrorPublisher;
import io.elastic.sailor.MessagePublisher;
import io.elastic.sailor.Utils;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorPublisherImpl implements ErrorPublisher {

    private static final Logger logger = LoggerFactory.getLogger(ErrorPublisherImpl.class);
    public static final String ERROR_PROPERTY = "error";
    public static final String ERROR_INPUT_PROPERTY = "errorInput";

    private MessagePublisher messagePublisher;
    private CryptoServiceImpl cipher;
    private String routingKey;
    private boolean noErrorsReply;

    @Inject
    public ErrorPublisherImpl(MessagePublisher messagePublisher,
                              CryptoServiceImpl cipher,
                              @Named(Constants.ENV_VAR_ERROR_ROUTING_KEY) String routingKey,
                              @Named(Constants.ENV_VAR_NO_ERROR_REPLIES) boolean noErrorsReply) {
        this.messagePublisher = messagePublisher;
        this.cipher = cipher;
        this.routingKey = routingKey;
        this.noErrorsReply = noErrorsReply;
    }

    @Override
    public void publish(Throwable e, AMQP.BasicProperties options, byte[] message) {

        final Object messageId = options.getHeaders().get(Constants.AMQP_HEADER_MESSAGE_ID);

        logger.warn("Caught an error in messageId={}. Publishing it to the error queue.", messageId, e);

        final String stackTrace = Utils.getStackTrace(e);

        final JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", e.getClass().getName())
                .add("stack", stackTrace);

        if (e.getMessage() != null) {
            builder.add("message", e.getMessage());
        }

        final JsonObject error = builder.build();

        final String encryptedError = toString(cipher.encryptJsonObject(error, MessageEncoding.BASE64));

        final JsonObjectBuilder payloadBuilder = Json.createObjectBuilder()
                .add(ErrorPublisherImpl.ERROR_PROPERTY, encryptedError);

        if (message != null) {
            final byte[] errorInput = createErrorInput(options, message);
            payloadBuilder.add(ErrorPublisherImpl.ERROR_INPUT_PROPERTY, toString(errorInput));
        }

        final JsonObject payload = payloadBuilder.build();

        byte[] errorPayload = payload.toString().getBytes();

        messagePublisher.publish(this.routingKey, errorPayload, options);

        sendHttpReplyIfRequired(encryptedError, options);

    }

    private void sendHttpReplyIfRequired(final String encryptedError, final AMQP.BasicProperties properties) {
        final Map<String, Object> headers = properties.getHeaders();
        final Object replyTo = headers.get(Constants.AMQP_HEADER_REPLY_TO);

        if (noErrorsReply) {
            return;
        }

        if (replyTo == null) {
            return;
        }

        final Map<String, Object> newHeaders = new HashMap<>(headers);
        newHeaders.put(Constants.AMQP_HEADER_ERROR_RESPONSE, true);

        final AMQP.BasicProperties newProperties = Utils.copy(properties)
                .headers(newHeaders)
                .build();

        messagePublisher.publish(replyTo.toString(), encryptedError.getBytes(), newProperties);
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
