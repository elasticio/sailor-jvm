package io.elastic.sailor.impl;

import io.elastic.sailor.AmqpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class GracefulShutdownHandler {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    private AmqpService amqp;
    private boolean isShutdownRequired;
    public AtomicInteger messagesProcessingCount = new AtomicInteger();
    private CountDownLatch exitSignal;

    public GracefulShutdownHandler(final AmqpService amqp, final boolean isShutdownRequired) {
        this.amqp = amqp;
        this.isShutdownRequired = isShutdownRequired;

        registerShutdownHook();
    }


    public void increment() {
        if (this.isShutdownRequired) {
            return;
        }
        logger.info("Incrementing the number of messages processed");
        this.messagesProcessingCount.incrementAndGet();
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
        if (this.isShutdownRequired) {
            return;
        }

        if (this.amqp == null) {
            return;
        }

        this.amqp.cancelConsumer();

        logger.info("Canceled all message consumers. Now waiting for all messages to be processed before exiting");

        this.exitSignal = new CountDownLatch(this.messagesProcessingCount.get());

        try {
            this.exitSignal.await();
        } catch (InterruptedException e) {
            logger.error(e.getMessage());
        }

        logger.info("No messages are being processed anymore. Exiting with 0");
        exit();
    }

    public void decrement() {

        if (this.isShutdownRequired) {
            return;
        }
        logger.info("Decrementing the number of messages processed");
        final int count = this.messagesProcessingCount.decrementAndGet();

        logger.info("{} messages are currently being processed", count);

        if(this.exitSignal != null){
            this.exitSignal.countDown();
        }
    }

    protected void exit() {
        System.exit(0);
    }

}
