package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class DataCallback extends CountingCallbackImpl {

    private static final Logger logger = LoggerFactory.getLogger(DataCallback.class);

    private MessagePublisher messagePublisher;
    private CryptoServiceImpl cipher;
    private ExecutionContext executionContext;
    private MessageResolver messageResolver;
    private String routingKey;
    private boolean emitLightweightMessage;
    private MessageEncoding messageEncoding;

    @Inject
    public DataCallback(
            @Assisted ExecutionContext executionContext,
            MessagePublisher messagePublisher,
            CryptoServiceImpl cipher,
            MessageResolver messageResolver,
            @Named(Constants.ENV_VAR_DATA_ROUTING_KEY) String routingKey,
            @Named(Constants.ENV_VAR_EMIT_LIGHTWEIGHT_MESSAGE) boolean emitLightweightMessage,
            @Named(Constants.ENV_VAR_PROTOCOL_VERSION) MessageEncoding messageEncoding) {
        this.executionContext = executionContext;
        this.messagePublisher = messagePublisher;
        this.cipher = cipher;
        this.messageResolver = messageResolver;
        this.routingKey = routingKey;
        this.emitLightweightMessage = emitLightweightMessage;
        this.messageEncoding = messageEncoding;
    }

    @Override
    public void receiveData(Object data) {
        logger.info("Step produced data to be published");

        // payload
        final Message message = (Message) data;

        JsonObject messageAsJson = executionContext.createPublisheableMessage(message);

        if (emitLightweightMessage) {
            messageAsJson = messageResolver.externalize(messageAsJson);
        }

        // encrypt
        byte[] encryptedPayload = cipher.encrypt(JSON.stringify(messageAsJson), messageEncoding);

        final AMQP.BasicProperties headers = createProperties(message);

        messagePublisher.publish(routingKey, encryptedPayload, headers);
    }

    private AMQP.BasicProperties createProperties(final Message message) {
        final AMQP.BasicProperties properties = executionContext.buildAmqpProperties(message.getId());

        final Map<String, Object> headers = new HashMap<>(properties.getHeaders());
        headers.put(Constants.AMQP_HEADER_PROTOCOL_VERSION, messageEncoding.protocolVersion);

        return Utils.buildAmqpProperties(headers);
    }
}
