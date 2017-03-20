package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExecutionContext {

    private final Step step;
    private final Message message;
    private final AMQP.BasicProperties amqpProperties;
    private final JsonObject passthrough;


    public ExecutionContext(
            final Step step,
            final Message message,
            final AMQP.BasicProperties amqpProperties) {
        this(step, message, amqpProperties, null);
    }

    public ExecutionContext(
            final Step step,
            final Message message,
            final AMQP.BasicProperties amqpProperties,
            final JsonObject passthrough) {
        this.step = step;
        this.message = message;
        this.amqpProperties = amqpProperties;
        this.passthrough = passthrough;
    }

    public Step getStep() {
        return this.step;
    }

    public Map<String, Object> buildDefaultHeaders() {

        final Map<String, Object> result = new HashMap<String, Object>();

        final Map<String, Object> headers = amqpProperties.getHeaders();
        result.put(Constants.AMQP_HEADER_EXEC_ID, headers.get(Constants.AMQP_HEADER_EXEC_ID));
        result.put(Constants.AMQP_HEADER_TASK_ID, headers.get(Constants.AMQP_HEADER_TASK_ID));
        result.put(Constants.AMQP_HEADER_USER_ID, headers.get(Constants.AMQP_HEADER_USER_ID));
        result.put(Constants.AMQP_HEADER_STEP_ID, this.step.getId());
        result.put(Constants.AMQP_HEADER_COMPONENT_ID, this.step.getCompId());
        result.put(Constants.AMQP_HEADER_FUNCTION, this.step.getFunction());
        result.put(Constants.AMQP_HEADER_START_TIMESTAMP, System.currentTimeMillis());

        final Object replyTo = headers.get(Constants.AMQP_HEADER_REPLY_TO);

        if (replyTo != null) {
            result.put(Constants.AMQP_HEADER_REPLY_TO, replyTo);
        }

        final Object parentMessageId = headers.get(Constants.AMQP_HEADER_MESSAGE_ID);

        if (parentMessageId != null) {
            result.put(Constants.AMQP_HEADER_PARENT_MESSAGE_ID, parentMessageId);
        }

        headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith(Constants.AMQP_META_HEADER_PREFIX))
                .forEach(entry -> result.put(entry.getKey().toLowerCase(), entry.getValue()));

        return result;
    }

    public AMQP.BasicProperties buildAmqpProperties() {
        return buildAmqpProperties(UUID.randomUUID());
    }

    public AMQP.BasicProperties buildAmqpProperties(final UUID messageId) {
        if (messageId == null) {
            throw new IllegalArgumentException("messageId is required");
        }

        final Map<String, Object> headers = buildDefaultHeaders();
        headers.put(Constants.AMQP_HEADER_MESSAGE_ID, messageId.toString());

        return Utils.buildAmqpProperties(headers);
    }

    public Message getMessage() {
        return message;
    }

    public Map<String, Object> getHeaders() {
        return amqpProperties.getHeaders();
    }

    public JsonObject createPassthroughMessage(final JsonObject message) {

        if (!this.step.isPassThroughRequired()) {
            return message;
        }

        final JsonObjectBuilder result = copyJsonObject(message);

        final JsonObjectBuilder passthroughBuilder = createPassthroughBuilder();

        passthroughBuilder.add(this.step.getId(), message);

        result.add(Constants.MESSAGE_PROPERTY_PASSTHROUGH, passthroughBuilder);

        return result.build();
    }

    private JsonObjectBuilder createPassthroughBuilder() {
        if (this.passthrough == null) {
            return Json.createObjectBuilder();
        }

        return copyJsonObject(this.passthrough);
    }

    private JsonObjectBuilder copyJsonObject(final JsonObject obj) {
        final JsonObjectBuilder result = Json.createObjectBuilder();
        obj.entrySet().forEach(s -> result.add(s.getKey(), s.getValue()));
        return result;
    }
}
