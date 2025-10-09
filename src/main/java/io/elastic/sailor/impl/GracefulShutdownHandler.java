package io.elastic.sailor.impl;

import io.elastic.sailor.AmqpService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class GracefulShutdownHandler {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    private AmqpService amqp;
    public AtomicInteger messagesProcessingCount = new AtomicInteger();
    private CountDownLatch exitSignal;

    private final CloseableHttpClient httpClient;

    public GracefulShutdownHandler(final AmqpService amqp, final CloseableHttpClient httpClient) {
        this.amqp = amqp;
        this.httpClient = httpClient;

        registerShutdownHook();
    }


    public void increment() {
        final int count = this.messagesProcessingCount.incrementAndGet();

        logger.debug("Incremented the number of messages concurrently processed to {}", count);
    }


    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutdown hook called. Will exit gracefully now");
                prepareGracefulShutdown();
            }
        });
        logger.debug("Registered a graceful shutdown hook");
    }

    protected void prepareGracefulShutdown() {
        logger.debug("Preparing graceful shutdown");

        if (this.amqp == null) {
            return;
        }

        // 1. Stop accepting new messages
        this.amqp.cancelConsumer();
        logger.debug("Canceled all message consumers.");

        // 2. Wait for in-flight messages to complete
        this.exitSignal = new CountDownLatch(this.messagesProcessingCount.get());
        final long messagesCount = this.exitSignal.getCount();
        if (messagesCount > 0) {
            logger.debug("Now waiting for {} messages to be processed before exiting", messagesCount);
        }
        try {
            this.exitSignal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        logger.debug("No messages are being processed anymore");

        // 3. Now that all work is done, shut down shared resources
        try {
            this.httpClient.close();
            logger.debug("Closed HTTP client");
        } catch (IOException e) {
            logger.error("Failed to close HTTP client", e);
        }

        amqp.disconnect();
    }

    public void decrement() {
        final int count = this.messagesProcessingCount.decrementAndGet();
        logger.debug("Decremented the number of messages concurrently processed to {}", count);

        if(this.exitSignal != null){
            this.exitSignal.countDown();
            logger.debug("Waiting for {} messages before exiting", exitSignal.getCount());
        }
    }

}
