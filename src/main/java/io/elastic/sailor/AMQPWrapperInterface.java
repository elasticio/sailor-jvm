package io.elastic.sailor;

import com.google.gson.JsonObject;

import java.util.Map;
import io.elastic.api.Message;

public interface AMQPWrapperInterface {

    void connect(String uri);
    void listenQueue(String queueName, String cipherKey, Sailor.Callback callback);

    void sendData(JsonObject data, Map<String,Object> headers);
    void sendError(Error err, final Map<String,Object> headers, Message originalMessage);
    void sendRebound(Error err, final Map<String,Object> headers, Message originalMessage);
    void sendSnapshot(JsonObject data, Map<String,Object> headers);
    void ack(Long deliveryTag);
    void reject(Long deliveryTag);
}
