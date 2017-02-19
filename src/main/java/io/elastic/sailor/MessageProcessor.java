package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;
import io.elastic.api.Module;

import java.util.Map;

public interface MessageProcessor {

    ExecutionStats processMessage(final Message incomingMessage,
                        final AMQP.BasicProperties amqpProperties,
                        final Module module);
}
