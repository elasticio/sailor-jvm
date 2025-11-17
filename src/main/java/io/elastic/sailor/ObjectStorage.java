package io.elastic.sailor;

import jakarta.json.JsonObject;

public interface ObjectStorage {

    JsonObject getJsonObject(String id);

    JsonObject postJsonObject(JsonObject object);

    JsonObject post(String object, String description);
}
