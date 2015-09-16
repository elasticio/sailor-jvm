package io.elastic.sailor.component;

import com.google.gson.JsonObject;
import io.elastic.api.SelectModelProvider;

public class SimpleSelectModelProvider implements SelectModelProvider {

    public static boolean SHOULD_FAIL = false;

    @Override
    public JsonObject getSelectModel(JsonObject configuration) {

        if (SHOULD_FAIL) {
            throw new RuntimeException("Spec author told me to fail");
        }

        final JsonObject result = new JsonObject();

        result.addProperty("de", "Germany");
        result.addProperty("us", "United States");
        result.add("cfg", configuration);

        return result;
    }
}
