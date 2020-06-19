package io.elastic.sailor;

import javax.json.JsonObject;

public interface ObjectStorage {

    JsonObject getJsonObject(String id);

    JsonObject postJsonObject(JsonObject object);
}
