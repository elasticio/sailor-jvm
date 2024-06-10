package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExecutionContext {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionContext.class);

    private final Step step;
    private byte[] rawMessage;
    private final Message message;
    private final AMQP.BasicProperties amqpProperties;
    private final ContainerContext containerContext;
    private final JsonObject snapshot;

    public ExecutionContext(
            final Step step,
            byte[] rawMessage,
            final Message message,
            final AMQP.BasicProperties amqpProperties,
            final ContainerContext containerContext,
            JsonObject snapshot
    ) {
        this.step = step;
        this.rawMessage = rawMessage;
        this.message = message;
        this.amqpProperties = amqpProperties;
        this.containerContext = containerContext;
        this.snapshot = snapshot;
    }

    public Step getStep() {
        return this.step;
    }

    public Map<String, Object> buildDefaultHeaders() {

        final Map<String, Object> result = new HashMap<String, Object>();

        final Map<String, Object> headers = amqpProperties.getHeaders();
        result.put(Constants.AMQP_HEADER_THREAD_ID, Utils.getThreadId(amqpProperties));
        result.put(Constants.AMQP_HEADER_CONTAINER_ID, this.containerContext.getContainerId());
        result.put(Constants.AMQP_HEADER_WORKSPACE_ID, this.containerContext.getWorkspaceId());
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

        final Object protocolVersion = headers.get(Constants.AMQP_HEADER_PROTOCOL_VERSION);

        if (protocolVersion != null) {

            result.put(Constants.AMQP_HEADER_PROTOCOL_VERSION, protocolVersion);
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


    public byte[] getRawMessage() {
        return rawMessage;
    }
    public Message getMessage() {
        return message;
    }
    public JsonObject getSnapshot() { return snapshot; }

    public Map<String, Object> getHeaders() {
        return amqpProperties.getHeaders();
    }

    public JsonObject createPublisheableMessage(final Message message) {

        final JsonObject messageAsJson = Utils.pick(message.toJsonObject(),
                Message.PROPERTY_ID,
                Message.PROPERTY_ATTACHMENTS,
                Message.PROPERTY_BODY,
                Message.PROPERTY_HEADERS,
                Message.PROPERTY_METHOD,
                Message.PROPERTY_ORIGINAL_URL,
                Message.PROPERTY_QUERY,
                Message.PROPERTY_URL);

        if (!this.step.isPassThroughRequired()) {
            return messageAsJson;
        }

        final JsonObjectBuilder result = createJsonObjectBuilder(messageAsJson);

        final JsonObjectBuilder passthroughBuilder = createPassthroughBuilder();

        if (this.step.isPutIncomingMessageIntoPassThrough()) {
            logger.info("Pass-through mode detected: incoming message");

            final Object previousStepId = this.amqpProperties.getHeaders().get(Constants.AMQP_HEADER_STEP_ID);

            if (previousStepId != null) {
                logger.info("Adding message of step '{}' into pass-through", previousStepId);

                final JsonObject incomingMessageWithoutPassThrough = Utils.pick(this.message.toJsonObject(),
                        Message.PROPERTY_ID,
                        Message.PROPERTY_ATTACHMENTS,
                        Message.PROPERTY_BODY,
                        Message.PROPERTY_HEADERS);

                passthroughBuilder.add(previousStepId.toString(), incomingMessageWithoutPassThrough);
            }
        } else {

            logger.info("Adding message of step '{}' into pass-through", this.step.getId());
            passthroughBuilder.add(this.step.getId(), messageAsJson);
        }

        result.add(Message.PROPERTY_PASSTHROUGH, passthroughBuilder);

        return result.build();
    }

    private JsonObjectBuilder createPassthroughBuilder() {
        if (this.message.getPassthrough() == null) {
            return Json.createObjectBuilder();
        }

        return createJsonObjectBuilder(this.message.getPassthrough());
    }

    private JsonObjectBuilder createJsonObjectBuilder(final JsonObject obj) {
        final JsonObjectBuilder result = Json.createObjectBuilder();
        obj.entrySet()
                .stream()
                .forEach(s -> result.add(s.getKey(), s.getValue()));
        return result;
    }
}
