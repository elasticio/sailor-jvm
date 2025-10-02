package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;

import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.elastic.sailor.AmqpService;
import io.elastic.sailor.Constants;
import io.elastic.sailor.MessagePublisher;

@Singleton
public class MessagePublisherImpl implements MessagePublisher {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessagePublisherImpl.class);
    private static final int WAIT_FOR_CONFIRM_DURATION = 5000;

    private String publishExchangeName;
    private AmqpService amqpService;
    private int publishMaxRetries;
    private long publishRetryDelay;
    private long publishMaxRetryDelay;
    private Channel publishChannel;
    private final boolean isPublishConfirmEnabled;
    private final boolean isPersistentMessagesEnabled;

    @Inject
    public MessagePublisherImpl(@Named(Constants.ENV_VAR_PUBLISH_MESSAGES_TO) final String publishExchangeName,
                                @Named(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_ATTEMPTS) int publishMaxRetries,
                                @Named(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_DELAY) long publishRetryDelay,
                                @Named(Constants.ENV_VAR_AMQP_PUBLISH_MAX_RETRY_DELAY) long publishMaxRetryDelay,
                                @Named(Constants.ENV_VAR_AMQP_PUBLISH_CONFIRM_ENABLED) boolean isPublishConfirmEnabled,
                                @Named(Constants.ENV_VAR_AMQP_AMQP_PERSISTENT_MESSAGES) boolean isPersistentMessagesEnabled,
                                final AmqpService amqpService) {
        this.publishExchangeName = publishExchangeName;
        this.publishMaxRetries = publishMaxRetries;
        this.publishRetryDelay = publishRetryDelay;
        this.publishMaxRetryDelay = publishMaxRetryDelay;
        this.amqpService = amqpService;
        this.isPublishConfirmEnabled = isPublishConfirmEnabled;
        this.isPersistentMessagesEnabled = isPersistentMessagesEnabled;
    }

    @Override
    public void publish(String routingKey, byte[] payload, AMQP.BasicProperties options) {

        logger.debug("Pushing to exchange={}, routingKey={}", this.publishExchangeName, routingKey);

        boolean retryPublish = true;
        int retryCount = 0;

        while (retryPublish) {
            final Channel publishChannel = getPublishChannel();
            AMQP.BasicProperties.Builder propertiesBuilder = getRetryProperties(options, retryCount);

            propertiesBuilder.deliveryMode(isPersistentMessagesEnabled ? 2 : 1); // 2 - persistent 1 - transient
            AMQP.BasicProperties properties = propertiesBuilder.build();
            logger.debug("isPersistentMessagesEnabled={}, isPublishConfirmEnabled={}, properties={}", isPersistentMessagesEnabled, isPublishConfirmEnabled, properties);

            try {
                logger.trace("Publish options={}, retryCount={}", this.publishExchangeName, routingKey, properties, options, retryCount);
                publishChannel.basicPublish(this.publishExchangeName, routingKey, properties, payload);
            } catch (IOException e) {
                throw new RuntimeException(String.format("Failed to publish message to exchange %s", publishExchangeName), e);
            }
            if (isPublishConfirmEnabled) {
                retryPublish = !waitForConfirms(publishChannel);
                if (retryPublish) {
                    sleep(retryCount + 1);

                    if (retryCount >= this.publishMaxRetries) {
                        throw new IllegalStateException(
                            String.format("Failed to publish the message to a queue after %s retries. " +
                                    "The limit of %s retries reached.",
                                retryCount, this.publishMaxRetries));
                    }
                    retryCount++;
                }
            } else {
                retryPublish = false;
            }
            logger.debug("Successfully published data to {}", this.publishExchangeName);
        }
    }

    private AMQP.BasicProperties.Builder getRetryProperties(final AMQP.BasicProperties properties, final int retryCount) {
        if (retryCount < 1) {
            return properties.builder();
        }

        final Map<String, Object> retryHeaders = new HashMap();
        retryHeaders.putAll(properties.getHeaders());
        retryHeaders.put(Constants.AMQP_HEADER_RETRY, retryCount);

        return properties.builder().headers(retryHeaders);
    }

    private boolean waitForConfirms(Channel publishChannel) {
        logger.debug("Waiting for publish confirm");
        try {
            return publishChannel.waitForConfirms(WAIT_FOR_CONFIRM_DURATION);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while waiting for publisher confirmation");
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException("Waiting for publisher confirmation timed out", e);
        } catch (IllegalStateException e) {
            logger.warn("Looks like publisher confirmation was asked on a non-Confirm channel. " +
                "Please check if the publisher channel was created with publisher confirms enabled.");
            throw e;
        }
    }

    private void sleep(int currentPublishAttempt) {
        long sleep = calculateSleepDuration(currentPublishAttempt);

        logger.warn("Published message to {} was not confirmed. Trying again in {} millis.",
            this.publishExchangeName, sleep);
        try {
            Thread.sleep(sleep);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while sleeping");
            throw new RuntimeException(e);
        }
    }

    private long calculateSleepDuration(int currentPublishAttempt) {
        double sleep = Math.pow(2, currentPublishAttempt - 1) * this.publishRetryDelay;

        if (sleep > this.publishMaxRetryDelay) {
            return this.publishMaxRetryDelay;
        }

        return new Double(sleep).longValue();
    }

    private Channel getPublishChannel() {
        if (this.publishChannel == null) {
            this.publishChannel = createPublishChannel();
        }
        return this.publishChannel;
    }

    private synchronized Channel createPublishChannel() {
        try {
            if (publishChannel == null) {
                final Connection connection = amqpService.getConnection();
                publishChannel = connection.createChannel();
                if (isPublishConfirmEnabled) {
                    publishChannel.confirmSelect();
                }
                logger.debug("Opened publish channel");
            }
            return publishChannel;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
