package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.AmqpService;
import io.elastic.sailor.ExecutionContext;

public class ErrorCallback extends CountingCallbackImpl {

    private ExecutionContext executionContext;
    private AmqpService amqp;
    private CryptoServiceImpl cipher;

    @Inject
    public ErrorCallback(
            @Assisted ExecutionContext executionContext,
            AmqpService amqp,
            CryptoServiceImpl cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    @Override
    public void receiveData(Object data) {
        amqp.sendError((Throwable) data, executionContext.buildAmqpProperties(), executionContext.getMessage());
    }
}
