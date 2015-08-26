package io.elastic.sailor;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.api.EventEmitter;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataCallback implements EventEmitter.Callback {

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
    public void receive(Object data) {
        logger.info("About to publish data to queue");

        // payload
        Message message = (Message) data;

        // encrypt
        byte[] encryptedPayload = cipher.encryptMessage(message).getBytes();

        amqp.sendData(encryptedPayload, executionContext.buildDefaultOptions());

        logger.info("Successfully published data to queue");
    }
}
