package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Executor;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class MessageConsumer extends DefaultConsumer {

    private static final Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private static final String HEADER_TASK_ID = "taskId";
    private static final String HEADER_STEP_ID = "stepId";

    /**
     * Constructs a new instance and records its association to the passed-in channel.
     *
     * @param channel the channel to which this consumer is attached
     */
    public MessageConsumer(final Channel channel) {
        super(channel);
    }

    @Override
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body)
            throws IOException {

        final Map<String, Object> headers = properties.getHeaders();
        final String taskId = headers.get(HEADER_TASK_ID).toString();
        final String stepId = headers.get(HEADER_STEP_ID).toString();

        long deliveryTag = envelope.getDeliveryTag();

        logger.info("{} consuming message deliveryTag={}", consumerTag, deliveryTag);

        //getChannel().basicAck(deliveryTag, false);

        final EventEmitter eventEmitter = new EventEmitter.Builder().build();

        final Executor executor = new Executor("componentClassName", eventEmitter);

        final Message message = createMessage();

        final ExecutionParameters params = new ExecutionParameters.Builder(message).build();

        executor.execute(params);
    }

    private static Message createMessage() {
        return new Message.Builder().build();
    }
}
