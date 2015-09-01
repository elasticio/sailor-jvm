package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Class to parse component.json
 * and to find there triggers and actions
 */

public final class ComponentResolver {
    private static final Logger logger = LoggerFactory.getLogger(ComponentResolver.class);

    private static final String FILENAME = "/component.json";

    private final JsonObject componentJson;

    public ComponentResolver() {
        componentJson = loadComponentJson();
    }

    private static JsonObject loadComponentJson() {

        logger.info("Component descriptor from classpath: {}", FILENAME);

        final InputStream stream = ComponentResolver.class
                .getResourceAsStream(FILENAME);

        if (stream == null) {
            throw new IllegalStateException(String.format(
                    "Component descriptor %s is not found in the classpath",
                    FILENAME));
        }

        InputStreamReader reader = null;

        try {
            reader = new InputStreamReader(stream);
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("Failed to close reader", e);
                }
            }
        }
    }

    /**
     * @param name - trigger or action name
     * @return name of Java class to execute for that trigger or action
     */
    public String findTriggerOrAction(String name) {

        final JsonObject object = findTriggerOrActionObject(name).getAsJsonObject();

        final JsonElement main = object.get("main");

        if (main == null) {
            throw new RuntimeException("Main class of '" + name + "' trigger/action is not specified");
        }

        return main.getAsString();
    }

    public JsonElement findTriggerOrActionObject(String name) {
        JsonObject result = null;

        final JsonObject triggers = componentJson.getAsJsonObject("triggers");
        final JsonObject actions = componentJson.getAsJsonObject("actions");

        if (triggers != null && triggers.get(name) != null) {
            result = triggers.getAsJsonObject(name);
        }
        if (actions != null && actions.get(name) != null) {
            result = actions.getAsJsonObject(name);
        }

        if (result == null) {
            throw new RuntimeException("'" + name + "' trigger or action is not found");
        }

        return result;
    }
}