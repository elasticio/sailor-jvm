package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.rabbitmq.client.AMQP;
import io.elastic.api.EventEmitter;
import io.elastic.api.HttpReply;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ExecutionContext;
import io.elastic.sailor.MessagePublisher;
import io.elastic.sailor.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class HttpReplyCallback implements EventEmitter.Callback {

    private static final Logger logger = LoggerFactory.getLogger(HttpReplyCallback.class);

    private MessagePublisher messagePublisher;;
    private CryptoServiceImpl cipher;
    private ExecutionContext executionContext;

    @Inject
    public HttpReplyCallback(
            final @Assisted ExecutionContext executionContext,
            final MessagePublisher messagePublisher,
            final CryptoServiceImpl cipher) {
        this.executionContext = executionContext;
        this.messagePublisher = messagePublisher;
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
        byte[] encryptedPayload = cipher.encryptJsonObject(payload, MessageEncoding.BASE64);

        sendHttpReply(encryptedPayload, createProperties());
    }

    private AMQP.BasicProperties createProperties() {
        final AMQP.BasicProperties properties = executionContext.buildAmqpProperties();

        final Map<String, Object> headers = new HashMap<>(properties.getHeaders());
        headers.put(Constants.AMQP_HEADER_PROTOCOL_VERSION, MessageEncoding.BASE64.protocolVersion);

        return Utils.buildAmqpProperties(headers);
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

    private void sendHttpReply(byte[] payload, AMQP.BasicProperties options) {
        final Map<String, Object> headers = options.getHeaders();
        final Object routingKey = headers.get("reply_to");

        if (routingKey == null) {
            throw new RuntimeException(
                    "Component emitted 'httpReply' event but 'reply_to' was not found in AMQP headers");
        }
        messagePublisher.publish(routingKey.toString(), payload, options);
    }
}
