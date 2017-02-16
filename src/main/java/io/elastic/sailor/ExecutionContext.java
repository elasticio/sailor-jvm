package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {

    private final Step step;
    private final Message message;
    private final Map<String, Object> headers;


    public ExecutionContext(
            final Step step,
            final Message message,
            final Map<String, Object> headers) {
        this.step = step;
        this.message = message;
        this.headers = headers;
    }

    public Step getStep() {
        return this.step;
    }

    public Map<String, Object> buildDefaultHeaders() {
        final Map<String, Object> result = new HashMap<String, Object>();

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

        return result;
    }

    public AMQP.BasicProperties buildDefaultOptions() {
        return Utils.buildAmqpProperties(buildDefaultHeaders());
    }


    public Message getMessage() {
        return message;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }
}
