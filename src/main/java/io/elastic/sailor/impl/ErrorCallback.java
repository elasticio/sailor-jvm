package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.ErrorPublisher;
import io.elastic.sailor.ExecutionContext;
import org.slf4j.LoggerFactory;

public class ErrorCallback extends CountingCallbackImpl {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(ErrorCallback.class);

    private ExecutionContext executionContext;
    private ErrorPublisher errorPublisher;

    @Inject
    public ErrorCallback(
            @Assisted ExecutionContext executionContext,
            ErrorPublisher errorPublisher) {
        this.executionContext = executionContext;
        this.errorPublisher = errorPublisher;
    }

    @Override
    public void receiveData(Object data) {
        logger.trace("Received error data");
        this.errorPublisher.publish((Throwable) data, executionContext.buildAmqpProperties(), executionContext.getRawMessage());
        logger.trace("Published error data");
    }
}
