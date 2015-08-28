package io.elastic.sailor.component;

import com.google.gson.JsonObject;
import io.elastic.api.SelectModelProvider;

public class SimpleSelectModelProvider implements SelectModelProvider {

    @Override
    public JsonObject getSelectModel(JsonObject configuration) {
        final JsonObject result = new JsonObject();

        result.addProperty("de", "Germany");
        result.addProperty("us", "United States");
        result.add("cfg", configuration);

        return result;
    }
}
