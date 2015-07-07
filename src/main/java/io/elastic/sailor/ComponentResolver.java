package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Class to parse component.json
 * and to find there triggers and actions
 */

public final class ComponentResolver {

    private static final String FILENAME = "component.json";
    private static final String USERDIR = System.getProperty("user.dir");

    private JsonObject componentJson;

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
            throw new RuntimeException("component.json is not found");
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
            throw new RuntimeException(name + " is not found");
        } else if (result.get("main") == null) {
            throw new RuntimeException("Main class of " + name + " is not specified");
        }

        return result.get("main").getAsString();
    }

    public String loadVerifyCredentials() {
        return "";
    }

    public String loadTriggerOrAction(String name) {
        findTriggerOrAction(name);
        return "";
    }
}