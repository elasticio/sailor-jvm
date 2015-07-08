package io.elastic.sailor;

import com.rabbitmq.client.AMQP;

public interface AMQPWrapperInterface {

    void connect(String uri);
    void listenQueue(String queueName, Sailor.Callback callback);

    void sendData(byte[] payload, AMQP.BasicProperties options);
    void sendError(byte[] payload, AMQP.BasicProperties options);
    void sendRebound(byte[] payload, AMQP.BasicProperties options);
    void sendSnapshot(byte[] payload, AMQP.BasicProperties options);

    void ack(Long deliveryTag);
    void reject(Long deliveryTag);
}