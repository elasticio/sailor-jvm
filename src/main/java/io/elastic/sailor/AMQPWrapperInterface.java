package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;
import io.elastic.api.Module;

public interface AMQPWrapperInterface {

    void connect();

    void subscribeConsumer(Module module);

    void cancelConsumer();

    void sendData(byte[] payload, AMQP.BasicProperties options);

    void sendHttpReply(byte[] payload, AMQP.BasicProperties options);

    void sendError(Throwable e, AMQP.BasicProperties options, Message originalMessage);

    void sendRebound(byte[] payload, AMQP.BasicProperties options);

    void sendSnapshot(byte[] payload, AMQP.BasicProperties options);

    void ack(Long deliveryTag);

    void reject(Long deliveryTag);
}