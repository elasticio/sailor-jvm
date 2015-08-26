package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.EventEmitter;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ErrorCallback implements EventEmitter.Callback {

    private ExecutionContext executionContext;
    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;

    public ErrorCallback(ExecutionContext executionContext, AMQPWrapperInterface amqp, CipherWrapper cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    @Override
    public void receive(Object data) {
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
