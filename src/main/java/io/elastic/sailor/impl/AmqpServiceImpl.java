package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.*;
import io.elastic.api.Function;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class AmqpServiceImpl implements AmqpService {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AmqpServiceImpl.class);

    private static AtomicInteger reconnectCount = new AtomicInteger();
    private static AtomicInteger recreateChannelCount = new AtomicInteger();

    private static final String CLIENT_PROPERTY_PRODUCT = "product";
    private static final String CLIENT_PROPERTY_VERSION = "version";

    private Connection connection;
    private Channel subscribeChannel;
    private String amqpUri;
    private String subscribeExchangeName;
    private Integer prefetchCount;
    private CryptoServiceImpl cipher;
    private MessageProcessor messageProcessor;
    private Step step;
    private String consumerTag;
    private ContainerContext containerContext;
    private MessageResolver messageResolver;
    private Function function;

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


    @Inject
    public void setFunction(@Named(Constants.NAME_FUNCTION_OBJECT) final Function function) {
        this.function = function;
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
            connection.close();
        } catch (IOException e) {
            logger.info("AMQP connection is already closed: " + e);
        }
        this.subscribeChannel = null;
        this.connection = null;
        logger.info("Successfully disconnected from AMQP");
    }

    public void subscribeConsumer() {
        final MessageConsumer consumer = new MessageConsumer(
                subscribeChannel, this.messageProcessor, this.function, step, this.containerContext, this.messageResolver);

        try {
            consumerTag = subscribeChannel.basicConsume(this.subscribeExchangeName, consumer, new ConsumerShutdownCallback());
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

    public void connect() {
        try {
            if (connection == null) {
                final ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(new URI(this.amqpUri));
                final Map<String, Object> clientProperties = factory.getClientProperties();
                clientProperties.put(CLIENT_PROPERTY_PRODUCT, "Java Sailor");
                if (this.containerContext != null) {

                    clientProperties.put(CLIENT_PROPERTY_VERSION, this.containerContext.getSailorVersion());
                }
                connection = factory.newConnection();
                logger.info("Connected to AMQP");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void reconnect() {
        final int count = AmqpServiceImpl.reconnectCount.incrementAndGet();
        logger.info("About to reconnect (#{}).", count);
        this.connection = null;
        this.connect();
    }

    public void createSubscribeChannel() {
        try {
            if (subscribeChannel == null) {
                subscribeChannel = connection.createChannel();
                subscribeChannel.basicQos(this.prefetchCount);
                logger.info("Opened subscribe channel");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void recreateSubscribeChannel() {
        final int count = AmqpServiceImpl.recreateChannelCount.incrementAndGet();
        logger.info("About to recreate channel (#{}).", count);
        this.subscribeChannel = null;
        this.createSubscribeChannel();
    }

    public Connection getConnection() {
        return this.connection;
    }

    private class ConsumerShutdownCallback implements ConsumerShutdownSignalCallback {

        @Override
        public void handleShutdownSignal(final String consumerTag, final ShutdownSignalException sig) {
            logger.info("Received AMQP shutdown signal for consumer {}", consumerTag);

            final boolean hardError = sig.isHardError();

            if (hardError) {
                // connection error
                logger.info("Consumer shutdown is caused by a connection error.");
                AmqpServiceImpl.this.reconnect();
            } else {
                // channel error
                logger.info("Consumer shutdown is caused by a channel error.");

            }
            AmqpServiceImpl.this.recreateSubscribeChannel();
            AmqpServiceImpl.this.subscribeConsumer();
        }
    }
}