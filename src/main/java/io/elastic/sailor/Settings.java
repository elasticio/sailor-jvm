package io.elastic.sailor;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Settings {

    private static final Logger logger = LoggerFactory.getLogger(Settings.class);

    private static final List<String> REQUIRED_SETTINGS = new ArrayList<String>(){{
        add("AMQP_URI");
        add("LISTEN_MESSAGES_ON");
        add("PUBLISH_MESSAGES_TO");
        add("DATA_ROUTING_KEY");
        add("ERROR_ROUTING_KEY");
        add("REBOUND_ROUTING_KEY");
        add("TASK");
        add("STEP_ID");
    }};

    private static final Map<String, String> OPTIONAL_SETTINGS = new HashMap<String, String>(){{
        put("REBOUND_INITIAL_EXPIRATION", "15000");
        put("REBOUND_LIMIT", "20");
        put("COMPONENT_PATH", "");
    }};

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
        for (String each : REQUIRED_SETTINGS) {
            if (settings.containsKey(each)) {
                result.put(each, settings.get(each));
                logger.info("Validated setting: " + each + " => " + settings.get(each));
            } else {
                throwError(each + " is missing");
            }
        }

        for (String each : OPTIONAL_SETTINGS.keySet()) {
            if (settings.containsKey(each)) {
                result.put(each, settings.get(each));
                logger.info("Validated setting: " + each + " => " + settings.get(each));
            } else {
                result.put(each, OPTIONAL_SETTINGS.get(each));
                logger.info("Validated setting: " + each + " => " + OPTIONAL_SETTINGS.get(each));
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

    public String getTriggerOrAction(){
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
        return thisStepNode.get("function").getAsString();
    }
}
