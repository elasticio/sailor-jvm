package io.elastic.sailor;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private String queueName = "task.worker.queue.origin";

    public static void main(String[] args) throws IOException {
        new Sailor().start();
    }

    public void start() throws IOException {
        logger.info("Starting up");

        final Connection connection = amqpConnect("amqp://guest:guest@localhost:5672");

        logger.info("Connected to AMQP successfully");

        final Channel channel = connection.createChannel();

        channel.basicConsume(queueName, false, new MessageConsumer(channel));
    }

    private static Connection amqpConnect(final String connectionUri) {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(connectionUri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            return factory.newConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
