package io.elastic.sailor;

import com.google.gson.JsonObject;

public final class ComponentResolver {

    private JsonObject componentJson = new JsonObject();

    public ComponentResolver(String componentPath){
        loadComponentJson(componentPath);
    }

    private void loadComponentJson(String componentPath){
        // @TODO load component json
    }

    public String findTriggerOrAction(String name){
        return componentJson.get("triggers").getAsJsonObject().get(name).getAsJsonObject().get("main").getAsString();
    }
}