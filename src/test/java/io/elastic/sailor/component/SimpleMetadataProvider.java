package io.elastic.sailor.component;

import io.elastic.api.DynamicMetadataProvider;

import javax.json.Json;
import javax.json.JsonObject;

public class SimpleMetadataProvider implements DynamicMetadataProvider {

    @Override
    public JsonObject getMetaModel(JsonObject configuration) {

        final JsonObject in = Json.createObjectBuilder()
                .add("type", "object")
                .build();
        final JsonObject out = Json.createObjectBuilder().build();

        return Json.createObjectBuilder()
                .add("in", in)
                .add("out", out).build();
    }
}
