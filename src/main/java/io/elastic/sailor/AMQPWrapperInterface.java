package io.elastic.sailor;

import com.google.gson.JsonObject;

import java.util.Map;

public interface AMQPWrapperInterface {

    void sendData(JsonObject data, Map<String,Object> headers);
    void sendError(Error err, final Map<String,Object> headers, JsonObject originalMessage);
    void sendRebound(JsonObject originalMessage, Map<String,Object> headers);
    void ack(JsonObject message);
    void reject(JsonObject message);
}
