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
        final Throwable t = (Throwable) data;

        final StringWriter writer = new StringWriter();
        t.printStackTrace(new PrintWriter(writer));

        final JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("name", t.getClass().getName())
                .add("stack", writer.toString());

        if (t.getMessage() != null) {
            builder.add("message", t.getMessage());
        }

        final JsonObject error = builder.build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("error", cipher.encryptJsonObject(error))
                .add("errorInput", cipher.encryptMessage(executionContext.getMessage()))
                .build();

        byte[] errorPayload = payload.toString().getBytes();

        amqp.sendError(errorPayload, executionContext.buildDefaultOptions());
    }
}
