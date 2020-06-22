package io.elastic.sailor;

import io.elastic.api.Message;

import javax.json.JsonObject;

public interface MessageResolver {

    Message materialize(byte[] body);

    JsonObject externalize(JsonObject object);
}
