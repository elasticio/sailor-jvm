package io.elastic.sailor;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.net.URI;

public final class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);

    private enum Required {
        AMQP_URI,
        LISTEN_MESSAGES_ON,
        PUBLISH_MESSAGES_TO,
        DATA_ROUTING_KEY,
        ERROR_ROUTING_KEY,
        REBOUND_ROUTING_KEY,
        TASK,
        STEP_ID
    }

    private enum Optional {
        REBOUND_INITIAL_EXPIRATION("15000"),
        REBOUND_LIMIT("20"),
        COMPONENT_PATH("");

        private final String defaultValue;
        Optional(String value) {
            this.defaultValue = value;
        }
    }

    private Map<String, String> sailorSettings;

    private JsonObject task;
    private String stepId;

    public Settings(Map<String, String> envVars) {
        sailorSettings = validateSettings(envVars);
        task = new JsonParser().parse(this.get("TASK")).getAsJsonObject();
        stepId = this.get("STEP_ID");
    }

    private Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> result = new HashMap<>();

        logger.info("About to validate settings...");
        for (Required each : Required.values()) {
            if (settings.containsKey(each.name())) {
                result.put(each.name(), settings.get(each.name()));
                logger.info("Validated setting: " + each + " => " + settings.get(each.name()));
            } else {
                throwError(each + " is missing");
            }
        }

        for (Optional each : Optional.values()) {
            if (settings.containsKey(each.name())) {
                result.put(each.name(), settings.get(each.name()));
                logger.info("Validated setting: " + each + " => " + settings.get(each.name()));
            } else {
                result.put(each.name(), each.defaultValue);
                logger.info("Validated setting: " + each + " => " + each.defaultValue);
            }
        }

        return result;
    }

    public String get(String key) {
        if (sailorSettings.get(key) == null) {
            throw new RuntimeException(key + " is not specified in settings");
        }
        return sailorSettings.get(key);
    }

    public int getInt(String key) {
        if (sailorSettings.get(key) == null) {
            throw new RuntimeException(key + " is not specified in settings");
        }
        return Integer.parseInt(sailorSettings.get(key));
    }

    public URI getURI(String key) {
        try {
            return new URI(sailorSettings.get(key));
        } catch (URISyntaxException e) {
            throw new RuntimeException("Invalid URI in " + key);
        }
    }

    private static void throwError(String message) {
        throw new IllegalArgumentException(message);
    }

    public JsonObject getTask(){
        return task;
    }

    public String getStepId(){
        return stepId;
    }

    public JsonObject getCfg(){
        if (task.get("data") != null && task.getAsJsonObject("data").get(stepId) != null) {
            return task.getAsJsonObject("data").getAsJsonObject(stepId);
        } else {
            return null;
        }
    }

    public JsonObject getSnapshot(){
        if (task.get("snapshot") != null && task.getAsJsonObject("snapshot").get(stepId) != null) {
            return task.getAsJsonObject("snapshot").getAsJsonObject(stepId);
        } else {
            return null;
        }
    }

    public JsonObject getTriggerOrAction(){
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

    public String getFunction(){
        return getTriggerOrAction().get("function").getAsString();
    }

    public String getCompId(){
        return getTriggerOrAction().get("compId").getAsString();
    }
}
