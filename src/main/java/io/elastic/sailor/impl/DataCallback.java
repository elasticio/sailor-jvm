package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.AmqpService;
import io.elastic.sailor.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;

public class DataCallback extends CountingCallbackImpl {

    private static final Logger logger = LoggerFactory.getLogger(DataCallback.class);

    private AmqpService amqp;
    private CryptoServiceImpl cipher;
    private ExecutionContext executionContext;

    @Inject
    public DataCallback(
            @Assisted ExecutionContext executionContext,
            AmqpService amqp,
            CryptoServiceImpl cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    @Override
    public void receiveData(Object data) {
        logger.info("Step produced data to be published");

        // payload
        final Message message = (Message) data;

        final JsonObject messageAsJson = executionContext.createPublisheableMessage(message);

        // encrypt
        byte[] encryptedPayload = cipher.encrypt(JSON.stringify(messageAsJson)).getBytes();

        amqp.sendData(encryptedPayload, executionContext.buildAmqpProperties(message.getId()));
    }
}
