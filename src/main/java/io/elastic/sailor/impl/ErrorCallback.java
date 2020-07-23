package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.ErrorPublisher;
import io.elastic.sailor.ExecutionContext;

public class ErrorCallback extends CountingCallbackImpl {

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
        this.errorPublisher.publish((Throwable) data, executionContext.buildAmqpProperties(), executionContext.getRawMessage());

    }
}
