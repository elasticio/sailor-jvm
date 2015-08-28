package io.elastic.sailor.component;

import com.google.gson.JsonObject;
import io.elastic.api.DynamicMetadataProvider;

public class SimpleMetadataProvider implements DynamicMetadataProvider {

    @Override
    public JsonObject getMetaModel(JsonObject configuration) {

        final JsonObject result = new JsonObject();

        final JsonObject in = new JsonObject();

        in.addProperty("type", "object");

        result.add("in", in);
        result.add("out", new JsonObject());

        return result;
    }
}
