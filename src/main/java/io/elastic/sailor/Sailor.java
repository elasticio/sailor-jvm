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
        Injector injector = Guice.createInjector(new SailorModule(), new SailorEnvironmentModule());

        Sailor sailor = injector.getInstance(Sailor.class);

        sailor.start();
    }

    @Inject
    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    public void start() throws IOException {
        logger.info("Starting up");
        amqp.connect();
        amqp.subscribeConsumer();
    }
}