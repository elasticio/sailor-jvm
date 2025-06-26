package io.elastic.sailor.impl;

import io.elastic.sailor.AmqpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class GracefulShutdownHandler {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    private AmqpService amqp;
    public AtomicInteger messagesProcessingCount = new AtomicInteger();
    private CountDownLatch exitSignal;

    public GracefulShutdownHandler(final AmqpService amqp) {
        this.amqp = amqp;

        registerShutdownHook();
    }


    public void increment() {
        final int count = this.messagesProcessingCount.incrementAndGet();

        logger.info("Incremented the number of messages concurrently processed to {}", count);
    }


    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutdown hook called. Will exit gracefully now");
                prepareGracefulShutdown();
            }
        });
        logger.info("Registered a graceful shutdown hook");
    }

    protected void prepareGracefulShutdown() {
        logger.info("Preparing graceful shutdown");

        HttpUtils.closeHttpClients();
        logger.info("Closed all HTTP clients");

        if (this.amqp == null) {
            return;
        }

        this.amqp.cancelConsumer();

        logger.info("Canceled all message consumers.");

        this.exitSignal = new CountDownLatch(this.messagesProcessingCount.get());

        final long messagesCount = this.exitSignal.getCount();

        if (messagesCount > 0) {
            logger.info("Now waiting for {} messages to be processed before exiting", messagesCount);
        }

        try {
            this.exitSignal.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }


        logger.info("No messages are being processed anymore");
        amqp.disconnect();
    }

    public void decrement() {
        final int count = this.messagesProcessingCount.decrementAndGet();
        logger.info("Decremented the number of messages concurrently processed to {}", count);

        if(this.exitSignal != null){
            this.exitSignal.countDown();
            logger.info("Waiting for {} messages before exiting", exitSignal.getCount());
        }
    }

}
