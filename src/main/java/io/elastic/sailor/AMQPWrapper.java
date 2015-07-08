package io.elastic.sailor;

import com.rabbitmq.client.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class AMQPWrapper implements AMQPWrapperInterface {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AMQPWrapper.class);

    private final Settings settings;
    private Connection amqp;
    private Channel subscribeChannel;
    private Channel publishChannel;

    public AMQPWrapper(Settings settings) {
        this.settings = settings;
    }

    public AMQPWrapper(Settings settings, Channel subscribeChannel, Channel publishChannel) {
        this.settings = settings;
        this.subscribeChannel = subscribeChannel;
        this.publishChannel = publishChannel;
    }

    public void connect(String link) {
        openConnection(link);
        openPublishChannel();
        openSubscribeChannel();
    }

    public void disconnect() {
        logger.info("About to disconnect from AMQP");
        try {
            subscribeChannel.close();
        } catch (IOException e) {
            logger.info("Subscription channel is already closed: " + e);
        }
        try {
            publishChannel.close();
        } catch (IOException e) {
            logger.info("Publish channel is already closed: " + e);
        }
        try {
            amqp.close();
        } catch (IOException e) {
            logger.info("AMQP connection is already closed: " + e);
        }
        logger.info("Successfully disconnected from AMQP");
    }

    public void listenQueue(String queueName, Sailor.Callback callback) {
        try {
            MessageConsumer consumer = new MessageConsumer(subscribeChannel, callback);
            subscribeChannel.basicConsume(queueName, consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void ack(Long deliveryTag) {
        try {
            logger.info(String.format("Message #%s ack", deliveryTag));
            subscribeChannel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reject(Long deliveryTag) {
        try {
            logger.info(String.format("Message #%s reject", deliveryTag));
            subscribeChannel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void sendData(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(settings.get("PUBLISH_MESSAGES_TO"), settings.get("DATA_ROUTING_KEY"), payload, options);
    }

    public void sendSnapshot(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(settings.get("PUBLISH_MESSAGES_TO"), settings.get("SNAPSHOT_ROUTING_KEY"), payload, options);
    }

    public void sendError(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(settings.get("PUBLISH_MESSAGES_TO"), settings.get("ERROR_ROUTING_KEY"), payload, options);
    }

    public void sendRebound(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(settings.get("PUBLISH_MESSAGES_TO"), settings.get("REBOUND_ROUTING_KEY"), payload, options);
    }

    private AMQPWrapper openConnection(String uri) {
        try {
            if (amqp == null) {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(new URI(uri));
                amqp = factory.newConnection();
                logger.info("Connected to AMQP");
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AMQPWrapper openPublishChannel() {
        try {
            if (publishChannel == null) {
                publishChannel = amqp.createChannel();
                logger.info("Opened publish channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AMQPWrapper openSubscribeChannel() {
        try {
            if (subscribeChannel == null) {
                subscribeChannel = amqp.createChannel();
                logger.info("Opened subscribe channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendToExchange(String exchangeName, String routingKey, byte[] payload, AMQP.BasicProperties options) {
        logger.info(String.format(
                "Pushing to exchange=%s, routingKey=%s, data=%s, options=%s",
                exchangeName, routingKey, new String(payload), options
        ));
        try {
            publishChannel.basicPublish(exchangeName, routingKey, options, payload);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to publish message to exchange %s, %s", exchangeName, e));
        }
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }
}
