package io.elastic.sailor;

import com.rabbitmq.client.AMQP;

public interface MessagePublisher {

    void publish(String routingKey, byte[] payload, AMQP.BasicProperties options);
}
