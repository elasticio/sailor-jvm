package io.elastic.sailor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import java.util.HashMap;
import java.util.Map;

public class ExecutionContext {

    private final String stepId;
    private final JsonObject task;
    private final Message message;
    private final Map<String, Object> headers;


    public ExecutionContext(
            final String stepId,
            final JsonObject task,
            final Message message,
            final Map<String, Object> headers) {
        this.stepId = stepId;
        this.task = task;
        this.message = message;
        this.headers = headers;
    }

    public String getStepId() {
        return this.stepId;
    }

    public String getCompId() {
        return getTriggerOrAction().get("compId").getAsString();
    }

    public String getFunction() {
        return getTriggerOrAction().get("function").getAsString();
    }

    public JsonObject getTriggerOrAction() {

        final String stepId = getStepId();

        JsonArray nodes = task.getAsJsonObject("recipe").getAsJsonArray("nodes");

        JsonObject thisStepNode = null;
        for (JsonElement node : nodes) {
            if (node.getAsJsonObject().get("id").getAsString().equals(stepId)) {
                thisStepNode = node.getAsJsonObject();
            }
        }

        if (thisStepNode == null) {
            throw new RuntimeException("Step " + stepId + " is not found in task recipe");
        }

        if (thisStepNode.get("function") == null) {
            throw new RuntimeException("Step " + stepId + " has no function specified");
        }

        return thisStepNode;
    }

    public JsonObject getCfg() {

        final String stepId = getStepId();

        if (task.get("data") != null && task.getAsJsonObject("data").get(stepId) != null) {
            return task.getAsJsonObject("data").getAsJsonObject(stepId);
        } else {
            return new JsonObject();
        }
    }

    public JsonObject getSnapshot() {

        final String stepId = getStepId();

        if (task.get("snapshot") != null && task.getAsJsonObject("snapshot").get(stepId) != null) {
            return task.getAsJsonObject("snapshot").getAsJsonObject(stepId);
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
        result.put(FeatureFlags.SKIP_MESSAGE_URL_DECODING, "1");

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
