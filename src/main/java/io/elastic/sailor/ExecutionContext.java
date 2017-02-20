package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {

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
        result.put("execId", headers.get("execId"));
        result.put("taskId", headers.get("taskId"));
        result.put("userId", headers.get("userId"));
        result.put("stepId", this.step.getId());
        result.put("compId", this.step.getCompId());
        result.put("function", this.step.getFunction());
        result.put("start", System.currentTimeMillis());

        final Object replyTo = headers.get("reply_to");

        if (replyTo != null) {
            result.put("reply_to", replyTo);
        }

        headers.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toLowerCase().startsWith(Constants.AMQP_META_HEADER_PREFIX))
                .forEach(entry -> result.put(entry.getKey().toLowerCase(), entry.getValue()));

        return result;
    }

    public AMQP.BasicProperties buildDefaultOptions() {
        return Utils.buildAmqpProperties(this.amqpProperties, buildDefaultHeaders());
    }

    public Message getMessage() {
        return message;
    }

    public Map<String, Object> getHeaders() {
        return amqpProperties.getHeaders();
    }
}
