package io.elastic.sailor;

import com.rabbitmq.client.AMQP;

public interface ErrorPublisher {

    void publish(Throwable e, AMQP.BasicProperties options, byte[] message);
}
