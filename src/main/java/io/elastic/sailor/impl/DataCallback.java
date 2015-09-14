package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.api.Message;
import io.elastic.sailor.AMQPWrapperInterface;
import io.elastic.sailor.CipherWrapper;
import io.elastic.sailor.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCallback extends CountingCallbackImpl {

    private static final Logger logger = LoggerFactory.getLogger(DataCallback.class);

    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;
    private ExecutionContext executionContext;

    @Inject
    public DataCallback(
            @Assisted ExecutionContext executionContext,
            AMQPWrapperInterface amqp,
            CipherWrapper cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    @Override
    public void receiveData(Object data) {
        logger.info("Step produced data to be published");

        // payload
        Message message = (Message) data;

        // encrypt
        byte[] encryptedPayload = cipher.encryptMessage(message).getBytes();

        amqp.sendData(encryptedPayload, executionContext.buildDefaultOptions());
    }
}
