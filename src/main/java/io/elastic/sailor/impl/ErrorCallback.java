package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.AMQPWrapperInterface;
import io.elastic.sailor.CipherWrapper;
import io.elastic.sailor.ExecutionContext;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorCallback extends CountingCallbackImpl {

    private ExecutionContext executionContext;
    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;

    @Inject
    public ErrorCallback(
            @Assisted ExecutionContext executionContext,
            AMQPWrapperInterface amqp,
            CipherWrapper cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    @Override
    public void receiveData(Object data) {
        amqp.sendError((Throwable) data, executionContext.buildDefaultOptions(), executionContext.getMessage());
    }
}
