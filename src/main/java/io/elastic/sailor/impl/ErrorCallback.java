package io.elastic.sailor.impl;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.AMQPWrapperInterface;
import io.elastic.sailor.CipherWrapper;
import io.elastic.sailor.ExecutionContext;
import io.elastic.sailor.impl.CountingCallbackImpl;

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
        Throwable t = (Throwable) data;

        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        JsonObject error = new JsonObject();
        error.addProperty("name", "Error");
        error.addProperty("message", t.getMessage());
        error.addProperty("stack", writer.toString());

        JsonObject payload = new JsonObject();
        payload.addProperty("error", cipher.encryptMessageContent(error));
        payload.addProperty("errorInput", cipher.encryptMessage(executionContext.getMessage()));

        byte[] errorPayload = payload.toString().getBytes();

        amqp.sendError(errorPayload, executionContext.buildDefaultOptions());
    }
}
