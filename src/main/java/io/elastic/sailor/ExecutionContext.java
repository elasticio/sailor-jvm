package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ExecutionContext {
    public final static String HEADER_PARENT_MESSAGE_ID = "parentMessageId";
    public final static String HEADER_REPLY_TO = "reply_to";
    public final static String HEADER_EXEC_ID = "execId";
    public final static String HEADER_TASK_ID = "taskId";
    public final static String HEADER_USER_ID = "userId";
    public final static String HEADER_STEP_ID = "stepId";
    public final static String HEADER_COMPONENT_ID = "compId";
    public final static String HEADER_FUNCTION = "function";
    public final static String HEADER_START_TIMESTAMP = "start";

    private final Step step;
    private final Message message;
    private final AMQP.BasicProperties amqpProperties;


    public ExecutionContext(
            final Step step,
            final Message message,
            final AMQP.BasicProperties amqpProperties) {
        this.step = step;
        this.message = message;
        this.amqpProperties = amqpProperties;
    }

    public Step getStep() {
        return this.step;
    }

    public Map<String, Object> buildDefaultHeaders() {

        final Map<String, Object> result = new HashMap<String, Object>();

        final Map<String, Object> headers = amqpProperties.getHeaders();
        result.put(HEADER_EXEC_ID, headers.get(HEADER_EXEC_ID));
        result.put(HEADER_TASK_ID, headers.get(HEADER_TASK_ID));
        result.put(HEADER_USER_ID, headers.get(HEADER_USER_ID));
        result.put(HEADER_STEP_ID, this.step.getId());
        result.put(HEADER_COMPONENT_ID, this.step.getCompId());
        result.put(HEADER_FUNCTION, this.step.getFunction());
        result.put(HEADER_START_TIMESTAMP, System.currentTimeMillis());

        final Object replyTo = headers.get(HEADER_REPLY_TO);

        if (replyTo != null) {
            result.put(HEADER_REPLY_TO, replyTo);
        }

        final Object parentMessageId = amqpProperties.getMessageId();

        if (parentMessageId != null) {
            result.put(HEADER_PARENT_MESSAGE_ID, parentMessageId);
        }

        headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith(Constants.AMQP_META_HEADER_PREFIX))
                .forEach(entry -> result.put(entry.getKey().toLowerCase(), entry.getValue()));

        return result;
    }

    public AMQP.BasicProperties buildDefaultOptions() {
        return buildDefaultOptions(UUID.randomUUID());
    }

    public AMQP.BasicProperties buildDefaultOptions(final UUID messageId) {
        return Utils.buildAmqpProperties(this.amqpProperties, messageId, buildDefaultHeaders());
    }

    public Message getMessage() {
        return message;
    }

    public Map<String, Object> getHeaders() {
        return amqpProperties.getHeaders();
    }
}
