package io.elastic.sailor;

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

    private static final String FILENAME = "component.json";
    private static final String USERDIR = System.getProperty("user.dir");

    private final JsonObject componentJson;

    /**
     * @param componentPath - path to the component, relative to sailor position
     */
    @Inject
    public ComponentResolver(
            @Named(Constants.ENV_VAR_COMPONENT_PATH) String componentPath) {
        componentJson = loadComponentJson(componentPath);
    }

    private JsonObject loadComponentJson(String componentPath) {

        logger.info("Component root directory: {}", componentPath);

        String componentFolder = new File(USERDIR, componentPath).getAbsolutePath();
        String componentJsonFile = new File(componentFolder, FILENAME).getAbsolutePath();

        logger.info("Loading component descriptor from file: {}", componentJsonFile);

        BufferedReader reader = null;

        try {
            reader = new BufferedReader(new FileReader(componentJsonFile));
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("component.json is not found in " + componentFolder);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("Failed to close file reader", e);
                }
            }
        }
    }

    /**
     * @param name - trigger or action name
     * @return name of Java class to execute for that trigger or action
     */
    public String findTriggerOrAction(String name) {
        JsonObject result = null;
        if (componentJson.get("triggers") != null && componentJson.getAsJsonObject("triggers").get(name) != null) {
            result = componentJson.getAsJsonObject("triggers").getAsJsonObject(name);
        }
        if (componentJson.get("actions") != null && componentJson.getAsJsonObject("actions").get(name) != null) {
            result = componentJson.getAsJsonObject("actions").getAsJsonObject(name);
        }

        if (result == null) {
            throw new RuntimeException("'" + name + "' trigger or action is not found");
        } else if (result.get("main") == null) {
            throw new RuntimeException("Main class of '" + name + "' trigger or action is not specified");
        }

        return result.get("main").getAsString();
    }
}