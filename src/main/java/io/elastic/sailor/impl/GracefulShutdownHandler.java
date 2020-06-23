package io.elastic.sailor.impl;

import io.elastic.sailor.AmqpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

public class GracefulShutdownHandler {

    private static final Logger logger = LoggerFactory.getLogger(GracefulShutdownHandler.class);

    private AmqpService amqp;
    private boolean isShutdownRequired;
    public AtomicInteger messagesProcessingCount = new AtomicInteger();

    public GracefulShutdownHandler(final AmqpService amqp, final boolean isShutdownRequired) {
        this.amqp = amqp;
        this.isShutdownRequired = isShutdownRequired;

        registerShutdownHook();
    }


    public void increment() {
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

    private void prepareGracefulShutdown() {
        if (GracefulShutdownHandler.this.isShutdownRequired) {
            return;
        }

        if (GracefulShutdownHandler.this.amqp == null) {
            return;
        }

        GracefulShutdownHandler.this.amqp.cancelConsumer();
    }

    public void decrementAndExit() {

        logger.info("Decrementing the number of messages processed");
        final int count = this.messagesProcessingCount.decrementAndGet();

        if (count < 1) {
            logger.info("No messages are being processed anymore. Exiting with 0");
            System.exit(0);
        }
    }

}
