package io.elastic.sailor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ExecutionDetails {

    private final JsonObject task;


    public ExecutionDetails() {
        task = ServiceSettings.getTask();
    }

    public String getStepId() {
        return ServiceSettings.getStepId();
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

    public JsonObject getCfg(){

        final String stepId = getStepId();

        if (task.get("data") != null && task.getAsJsonObject("data").get(stepId) != null) {
            return task.getAsJsonObject("data").getAsJsonObject(stepId);
        } else {
            return new JsonObject();
        }
    }

    public JsonObject getSnapshot(){

        final String stepId = getStepId();

        if (task.get("snapshot") != null && task.getAsJsonObject("snapshot").get(stepId) != null) {
            return task.getAsJsonObject("snapshot").getAsJsonObject(stepId);
        } else {
            return new JsonObject();
        }
    }
}
