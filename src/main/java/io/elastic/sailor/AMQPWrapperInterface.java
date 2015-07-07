package io.elastic.sailor;

import com.google.gson.JsonObject;

import java.util.Map;

public interface AMQPWrapperInterface {

    void connect(String uri);
    void listenQueue(String queueName, String cipherKey, Sailor.Callback callback);

    void sendData(JsonObject data, Map<String,Object> headers);
    void sendError(Error err, final Map<String,Object> headers, JsonObject originalMessage);
    void sendRebound(JsonObject originalMessage, Map<String,Object> headers);
    void ack(Long deliveryTag);
    void reject(Long deliveryTag);
}