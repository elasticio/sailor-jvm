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
    private ComponentResolver componentResolver;
    private CipherWrapper cipher;

    @Inject
    public Sailor(final ComponentResolver componentResolver,
                  final CipherWrapper cipher) {
        this.componentResolver = componentResolver;
        this.cipher = cipher;
    }

    public static void main(String[] args) throws IOException {
        Injector injector = Guice.createInjector(new SailorModule(), new EnvironmentModule());

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
        final MessageProcessor processor = new MessageProcessor(amqp, cipher, componentResolver);
        amqp.subscribeConsumer(processor);
        logger.info("Connected to AMQP successfully");
    }
}