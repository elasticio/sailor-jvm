package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {

    private final String stepId;
    private final JsonObject step;
    private final Message message;
    private final Map<String, Object> headers;


    public ExecutionContext(
            final String stepId,
            final JsonObject step,
            final Message message,
            final Map<String, Object> headers) {
        this.stepId = stepId;
        this.step = step;
        this.message = message;
        this.headers = headers;
    }

    public String getStepId() {
        return this.stepId;
    }

    public String getCompId() {
        return this.step.get(Constants.STEP_PROPERTY_COMP_ID).getAsString();
    }

    public String getFunction() {
        return this.step.get(Constants.STEP_PROPERTY_FUNCTION).getAsString();
    }

    public JsonObject getCfg() {

        return getAsNullSafeObject(Constants.STEP_PROPERTY_CFG);
    }

    public JsonObject getSnapshot() {

        return getAsNullSafeObject(Constants.STEP_PROPERTY_SNAPSHOT);
    }

    private JsonObject getAsNullSafeObject(final String name) {

        final JsonElement value = this.step.get(name);

        if (value != null) {
            return value.getAsJsonObject();
        } else {
            return new JsonObject();
        }
    }

    public Map<String, Object> buildDefaultHeaders() {
        final Map<String, Object> result = new HashMap<String, Object>();

        result.put("execId", headers.get("execId"));
        result.put("taskId", headers.get("taskId"));
        result.put("userId", headers.get("userId"));
        result.put("stepId", getStepId());
        result.put("compId", getCompId());
        result.put("function", getFunction());
        result.put("start", System.currentTimeMillis());

        return result;
    }

    public AMQP.BasicProperties buildDefaultOptions() {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(buildDefaultHeaders())
                .priority(1)// this should equal to mandatory true
                .deliveryMode(2)//TODO: check if flag .mandatory(true) was set
                .build();
    }


    public Message getMessage() {
        return message;
    }

    public Map<String, Object> getHeaders() {
        return headers;
    }
}
