package io.elastic.sailor;

import com.rabbitmq.client.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;

public class AMQPWrapper implements AMQPWrapperInterface {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AMQPWrapper.class);

    private Connection amqp;
    private Channel subscribeChannel;
    private Channel publishChannel;

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

    public void listenQueue(String queueName, CipherWrapper cipher, Sailor.Callback callback) {
        try {
            MessageConsumer consumer = new MessageConsumer(subscribeChannel, cipher, callback);
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
        sendToExchange(ServiceSettings.getDataRoutingKey(), payload, options);
    }

    public void sendSnapshot(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(ServiceSettings.getSnapshotRoutingKey(), payload, options);
    }

    public void sendError(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(ServiceSettings.getErrorRoutingKey(), payload, options);
    }

    public void sendRebound(byte[] payload, AMQP.BasicProperties options) {
        sendToExchange(ServiceSettings.getReboundRoutingKey(), payload, options);
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

    private void sendToExchange(String routingKey, byte[] payload, AMQP.BasicProperties options) {
        final String exchangeName = ServiceSettings.getPublishMessagesTo();

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



    public void setSubscribeChannel(Channel subscribeChannel) {
        this.subscribeChannel = subscribeChannel;
    }

    public void setPublishChannel(Channel publishChannel) {
        this.publishChannel = publishChannel;
    }
}