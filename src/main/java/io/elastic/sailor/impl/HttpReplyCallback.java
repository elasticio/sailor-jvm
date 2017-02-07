package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.api.EventEmitter;
import io.elastic.api.HttpReply;
import io.elastic.sailor.AMQPWrapperInterface;
import io.elastic.sailor.CipherWrapper;
import io.elastic.sailor.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class HttpReplyCallback implements EventEmitter.Callback {

    private static final Logger logger = LoggerFactory.getLogger(DataCallback.class);

    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;
    private ExecutionContext executionContext;

    @Inject
    public HttpReplyCallback(
            final @Assisted ExecutionContext executionContext,
            final AMQPWrapperInterface amqp,
            final CipherWrapper cipher) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
    }

    @Override
    public void receive(Object data) {
        final HttpReply reply = (HttpReply) data;

        final JsonObjectBuilder headers = Json.createObjectBuilder();
        reply.getHeaders().entrySet().stream().forEach(entry -> headers.add(entry.getKey(), entry.getValue()));

        final JsonObject payload = Json.createObjectBuilder()
                .add("statusCode", reply.getStatus())
                .add("body", getContentAsString(reply))
                .add("headers", headers.build())
                .build();
        // encrypt
        byte[] encryptedPayload = cipher.encryptJsonObject(payload).getBytes();

        amqp.sendHttpReply(encryptedPayload, executionContext.buildDefaultOptions());
    }

    private String getContentAsString(final HttpReply reply) {
        try {
            return inputStreamAsString(reply.getContent());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String inputStreamAsString(InputStream input) throws IOException {
        final StringWriter output = new StringWriter();
        final char[] buffer = new char[1024 * 4];
        final InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);

        int n = 0;
        while (-1 != (n = reader.read(buffer))) {
            output.write(buffer, 0, n);
        }

        return output.toString();
    }
}
