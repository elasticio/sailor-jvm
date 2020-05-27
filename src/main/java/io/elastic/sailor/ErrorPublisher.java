package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

public interface ErrorPublisher {

    void publish(Throwable e, AMQP.BasicProperties options, Message originalMessage);
}
