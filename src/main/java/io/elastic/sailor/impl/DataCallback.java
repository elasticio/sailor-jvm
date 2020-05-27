package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ExecutionContext;
import io.elastic.sailor.MessagePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;

public class DataCallback extends CountingCallbackImpl {

    private static final Logger logger = LoggerFactory.getLogger(DataCallback.class);

    private MessagePublisher messagePublisher;
    private CryptoServiceImpl cipher;
    private ExecutionContext executionContext;
    private String routingKey;

    @Inject
    public DataCallback(
            @Assisted ExecutionContext executionContext,
            MessagePublisher messagePublisher,
            CryptoServiceImpl cipher,
            @Named(Constants.ENV_VAR_DATA_ROUTING_KEY) String routingKey) {
        this.executionContext = executionContext;
        this.messagePublisher = messagePublisher;
        this.cipher = cipher;
        this.routingKey = routingKey;
    }

    @Override
    public void receiveData(Object data) {
        logger.info("Step produced data to be published");

        // payload
        final Message message = (Message) data;

        final JsonObject messageAsJson = executionContext.createPublisheableMessage(message);

        // encrypt
        byte[] encryptedPayload = cipher.encrypt(JSON.stringify(messageAsJson)).getBytes();

        messagePublisher.publish(routingKey, encryptedPayload, executionContext.buildAmqpProperties(message.getId()));
    }
}
