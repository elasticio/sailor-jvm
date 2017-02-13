package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private AMQPWrapperInterface amqp;

    public static void main(String[] args) throws IOException {
        createAndStartSailor();
    }

    static Sailor createAndStartSailor() throws IOException {
        Injector injector = Guice.createInjector(
                new SailorModule(), new SailorEnvironmentModule());

        final Sailor sailor =  injector.getInstance(Sailor.class);

        sailor.start();

        logger.info("Sailor started");

        return sailor;
    }

    @Inject
    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    public void start() throws IOException {
        logger.info("Connecting to AMQP");
        amqp.connect();

        logger.info("Subscribing to queues");
        amqp.subscribeConsumer();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutdown hook called");
            }
        });
    }
}