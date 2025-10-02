package io.elastic.sailor.impl;

import io.elastic.sailor.AmqpService;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

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
        try {
            this.httpClient.close();
            logger.debug("Closed HTTP client");
        } catch (IOException e) {
            logger.error("Failed to close HTTP client", e);
        }

        if (this.amqp == null) {
            return;
        }

        this.amqp.cancelConsumer();

        logger.debug("Canceled all message consumers.");

        this.exitSignal = new CountDownLatch(this.messagesProcessingCount.get());

        final long messagesCount = this.exitSignal.getCount();

        if (messagesCount > 0) {
            logger.debug("Now waiting for {} messages to be processed before exiting", messagesCount);
        }

        try {
            this.exitSignal.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }


        logger.debug("No messages are being processed anymore");
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
