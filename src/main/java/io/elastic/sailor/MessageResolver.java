package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import jakarta.json.JsonObject;

public interface MessageResolver {

    Message materialize(byte[] body, AMQP.BasicProperties properties);

    JsonObject externalize(JsonObject object);
}
