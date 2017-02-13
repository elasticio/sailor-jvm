package io.elastic.sailor;

import com.rabbitmq.client.AMQP;

public interface AMQPWrapperInterface {

    void connect();

    void subscribeConsumer();

    void cancelConsumer();

    void sendData(byte[] payload, AMQP.BasicProperties options);

    void sendHttpReply(byte[] payload, AMQP.BasicProperties options);

    void sendError(byte[] payload, AMQP.BasicProperties options);

    void sendRebound(byte[] payload, AMQP.BasicProperties options);

    void sendSnapshot(byte[] payload, AMQP.BasicProperties options);

    void ack(Long deliveryTag);

    void reject(Long deliveryTag);
}