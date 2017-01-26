package io.elastic.sailor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
            return Json.createReader(reader).readObject();
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

    public String findCredentialsVerifier() {

        final JsonObject credentials = componentJson.getJsonObject("credentials");

        if (credentials == null) {
            return null;
        }

        final JsonString verifier = credentials.getJsonString("verifier");

        if (verifier == null) {
            return null;
        }

        return verifier.getString();
    }

    /**
     * @param name - trigger or action name
     * @return name of Java class to execute for that trigger or action
     */
    public String findTriggerOrAction(String name) {

        final JsonObject object = findTriggerOrActionObject(name);

        final JsonString main = object.getJsonString("main");

        if (main == null) {
            throw new RuntimeException("Main class of '" + name + "' trigger/action is not specified");
        }

        return main.getString();
    }

    public JsonObject findTriggerOrActionObject(String name) {
        JsonObject result = null;

        final JsonObject triggers = componentJson.getJsonObject("triggers");
        final JsonObject actions = componentJson.getJsonObject("actions");

        if (triggers != null && triggers.get(name) != null) {
            result = triggers.getJsonObject(name);
        }
        if (actions != null && actions.get(name) != null) {
            result = actions.getJsonObject(name);
        }

        if (result == null) {
            throw new RuntimeException("'" + name + "' trigger or action is not found");
        }

        return result;
    }
}