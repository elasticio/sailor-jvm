package io.elastic.sailor;

import com.rabbitmq.client.Connection;

public interface AmqpService {

    void connect();

    void reconnect();

    void createSubscribeChannel();

    void recreateSubscribeChannel();

    void disconnect();

    void subscribeConsumer();

    void cancelConsumer();

    void ack(Long deliveryTag);

    void reject(Long deliveryTag);

    Connection getConnection();
}