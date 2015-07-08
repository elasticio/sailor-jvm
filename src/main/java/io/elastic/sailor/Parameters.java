package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class Parameters {
    private JsonObject params;

    public Parameters(JsonObject params) {
        this.params = params;
    }

    public JsonElement get(String key) {
        return params.get(key);
    }

    public String getAsString(String key) {
        return get(key).getAsString();
    }
}
