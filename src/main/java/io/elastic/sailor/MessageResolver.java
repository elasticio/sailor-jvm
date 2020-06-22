package io.elastic.sailor;

import io.elastic.api.Message;

import javax.json.JsonObject;

public interface MessageResolver {

    Message resolve(byte[] body);

    JsonObject externalize(JsonObject object);
}
