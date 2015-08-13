package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;

/**
 * Class to parse component.json
 * and to find there triggers and actions
 */

public final class ComponentResolver {

    private static final String FILENAME = "component.json";
    private static final String USERDIR = System.getProperty("user.dir");

    private final JsonObject componentJson;

    /**
     * @param componentPath - path to the component, relative to sailor position
     */
    public ComponentResolver(String componentPath){
        componentJson = loadComponentJson(componentPath);
    }

    private JsonObject loadComponentJson(String componentPath){

        String componentFolder = new File(USERDIR, componentPath).getAbsolutePath();
        String componentJsonFile = new File(componentFolder, FILENAME).getAbsolutePath();

        try {
            BufferedReader br = new BufferedReader(new FileReader(componentJsonFile));
            JsonParser parser = new JsonParser();
            return parser.parse(br).getAsJsonObject();
        } catch (FileNotFoundException e) {
            throw new RuntimeException("component.json is not found in " + componentFolder);
        }
    }

    /**
     *
     * @param name - trigger or action name
     * @return name of Java class to execute for that trigger or action
     */
    public String findTriggerOrAction(String name){
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

    public Class loadTriggerOrAction(String triggerOrActionName) {
        try {
            String className = findTriggerOrAction(triggerOrActionName);
            return Class.forName(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Class loadVerifyCredentials() throws ClassNotFoundException {
        String className = componentJson.getAsJsonObject("credentials").get("main").getAsString();
        return Class.forName(className);
    }
}