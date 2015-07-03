package io.elastic.sailor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.elastic.api.ExecutionParameters;
import io.elastic.demo.*;

public class TaskExecutor {

    private static final int TIMEOUT = System.getenv("TIMEOUT")!=null && Integer.parseInt(System.getenv("TIMEOUT"))>0 ?
            Integer.parseInt(System.getenv("TIMEOUT")) : 20 * 60 * 1000;

    public TaskExecutor() {

    }

    public void execute(ExecutionParameters params){

        JsonObject task = getTask();
        String stepId = getStepId();

        JsonObject cfg = getStepCfg(task, stepId);
        JsonObject stepInfo = getStepInfo(task, stepId);
        String function = stepInfo.get("function").getAsString();






        // 1. Find component by .env vars
        // 2. Find function to execute
        // 3. Pass data, configuration, snapshot
        // 4. Listen for events
    }

    private JsonObject getTask(){
        String task = System.getenv("MESSAGE_CRYPTO_PASSWORD");
        return new JsonParser().parse(task).getAsJsonObject();
    }

    private String getStepId(){
        return System.getenv("STEP_ID");
    }

    private JsonObject getStepCfg(JsonObject task, String stepId){
        return task.get("data").getAsJsonObject().get(stepId).getAsJsonObject();
    }

    private JsonObject getStepInfo(JsonObject task, String stepId){
        JsonArray nodes = task.get("recipe").getAsJsonObject().get("nodes").getAsJsonArray();
        for (JsonElement node : nodes) {
            if (node.getAsJsonObject().get("id").getAsString().equals(stepId)) {
                return node.getAsJsonObject();
            }
        }
        return null;
    }

    public static void main(String[] args) {
        System.out.println("Hello, World");
    }
}
