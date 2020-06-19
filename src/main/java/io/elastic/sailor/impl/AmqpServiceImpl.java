package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import io.elastic.api.Function;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.TimeoutException;

@Singleton
public class AmqpServiceImpl implements AmqpService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AmqpServiceImpl.class);

    private Connection amqp;
    private Channel subscribeChannel;
    private Channel publishChannel;

    private String amqpUri;
    private String subscribeExchangeName;
    private Integer prefetchCount;
    private CryptoServiceImpl cipher;
    private MessageProcessor messageProcessor;
    private Step step;
    private String consumerTag;
    private ContainerContext containerContext;
    private MessageResolver messageResolver;

    @Inject
    public AmqpServiceImpl(CryptoServiceImpl cipher) {
        this.cipher = cipher;
    }

    @Inject
    public void setAmqpUri(
            @Named(Constants.ENV_VAR_AMQP_URI) String amqpUri) {
        this.amqpUri = amqpUri;
    }

    @Inject
    public void setSubscribeExchangeName(
            @Named(Constants.ENV_VAR_LISTEN_MESSAGES_ON) String subscribeExchangeName) {
        this.subscribeExchangeName = subscribeExchangeName;
    }

    @Inject
    public void setMessageProcessor(MessageProcessor messageProcessor) {
        this.messageProcessor = messageProcessor;
    }

    @Inject
    public void setPrefetchCount(
            @Named(Constants.ENV_VAR_RABBITMQ_PREFETCH_SAILOR) Integer prefetchCount) {
        this.prefetchCount = prefetchCount;
    }

    @Inject
    public void setStep(@Named(Constants.NAME_STEP_JSON) Step step) {
        this.step = step;
    }


    @Inject
    public void setContainerContext(ContainerContext containerContext) {
        this.containerContext = containerContext;
    }

    @Inject
    public void setMessageResolver(final MessageResolver messageResolver) {
        this.messageResolver = messageResolver;
    }

    public void connectAndSubscribe() {
        openConnection();
        openSubscribeChannel();
    }

    @Override
    public void disconnect() {
        logger.info("About to disconnect from AMQP");
        try {
            subscribeChannel.close();
        } catch (IOException | TimeoutException e) {
            logger.info("Subscription channel is already closed: " + e);
        }
        try {
            publishChannel.close();
        } catch (IOException | TimeoutException e) {
            logger.info("Publish channel is already closed: " + e);
        }
        try {
            amqp.close();
        } catch (IOException e) {
            logger.info("AMQP connection is already closed: " + e);
        }
        logger.info("Successfully disconnected from AMQP");
    }

    public void subscribeConsumer(final Function function) {
        final MessageConsumer consumer = new MessageConsumer(
                subscribeChannel, cipher, this.messageProcessor, function, step, this.containerContext, this.messageResolver);

        try {
            consumerTag = subscribeChannel.basicConsume(this.subscribeExchangeName, consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("Subscribed consumer {}. Waiting for messages to arrive ...", consumerTag);
    }

    public void cancelConsumer() {
        if (consumerTag != null) {
            logger.info("Canceling consumer {}", consumerTag);
            try {
                subscribeChannel.basicCancel(consumerTag);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        consumerTag = null;
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

    private AmqpServiceImpl openConnection() {
        try {
            if (amqp == null) {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(new URI(this.amqpUri));
                amqp = factory.newConnection();
                logger.info("Connected to AMQP");
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AmqpServiceImpl openSubscribeChannel() {
        try {
            if (subscribeChannel == null) {
                subscribeChannel = amqp.createChannel();
                subscribeChannel.basicQos(this.prefetchCount);
                logger.info("Opened subscribe channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setSubscribeChannel(Channel subscribeChannel) {
        this.subscribeChannel = subscribeChannel;
    }

    public void setPublishChannel(Channel publishChannel) {
        this.publishChannel = publishChannel;
    }

    public Connection getConnection() {
        return this.amqp;
    }
}