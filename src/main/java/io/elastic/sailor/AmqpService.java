package io.elastic.sailor;

import com.rabbitmq.client.Connection;
import io.elastic.api.Function;

public interface AmqpService {

    void connectAndSubscribe();

    void disconnect();

    void subscribeConsumer(Function function);

    void cancelConsumer();

    void ack(Long deliveryTag);

    void reject(Long deliveryTag);

    Connection getConnection();
}