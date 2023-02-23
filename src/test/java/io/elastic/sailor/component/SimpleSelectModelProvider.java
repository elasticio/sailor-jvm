package io.elastic.sailor.component;

import io.elastic.api.SelectModelProvider;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class SimpleSelectModelProvider implements SelectModelProvider {

    public static boolean SHOULD_FAIL = false;

    @Override
    public JsonObject getSelectModel(JsonObject configuration) {

        if (SHOULD_FAIL) {
            throw new RuntimeException("Spec author told me to fail");
        }

        final JsonObject result = Json.createObjectBuilder()
                .add("de", "Germany")
                .add("us", "United States")
                .add("cfg", configuration)
                .build();

        return result;
    }
}
