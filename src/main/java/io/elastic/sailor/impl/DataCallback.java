package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ExecutionContext;
import io.elastic.sailor.MessagePublisher;
import io.elastic.sailor.MessageResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;

public class DataCallback extends CountingCallbackImpl {

    private static final Logger logger = LoggerFactory.getLogger(DataCallback.class);

    private MessagePublisher messagePublisher;
    private CryptoServiceImpl cipher;
    private ExecutionContext executionContext;
    private MessageResolver messageResolver;
    private String routingKey;
    private boolean emitLightweightMessage;

    @Inject
    public DataCallback(
            @Assisted ExecutionContext executionContext,
            MessagePublisher messagePublisher,
            CryptoServiceImpl cipher,
            MessageResolver messageResolver,
            @Named(Constants.ENV_VAR_DATA_ROUTING_KEY) String routingKey,
            @Named(Constants.ENV_VAR_EMIT_LIGHTWEIGHT_MESSAGE) boolean emitLightweightMessage) {
        this.executionContext = executionContext;
        this.messagePublisher = messagePublisher;
        this.cipher = cipher;
        this.messageResolver = messageResolver;
        this.routingKey = routingKey;
        this.emitLightweightMessage = emitLightweightMessage;
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
        byte[] encryptedPayload = cipher.encrypt(JSON.stringify(messageAsJson)).getBytes();

        messagePublisher.publish(routingKey, encryptedPayload, executionContext.buildAmqpProperties(message.getId()));
    }
}
