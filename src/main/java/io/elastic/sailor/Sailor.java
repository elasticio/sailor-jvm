package io.elastic.sailor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private AMQPWrapperInterface amqp;
    private ComponentResolver componentResolver;
    private CipherWrapper cipher;

    public Sailor(final ComponentResolver componentResolver,
                  final CipherWrapper cipher,
                  final AMQPWrapperInterface amqp) {
        this.componentResolver = componentResolver;
        this.cipher = cipher;
        this.amqp = amqp;
    }

    public static void main(String[] args) throws IOException {
        final ComponentResolver componentResolver
                = new ComponentResolver(ServiceSettings.getComponentPath());

        final CipherWrapper cipher = new CipherWrapper(
                ServiceSettings.getMessageCryptoPasswort(), ServiceSettings.getMessageCryptoIV());

        Sailor sailor = new Sailor(componentResolver, cipher, new AMQPWrapper(cipher));

        sailor.start();
    }

    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    public void start() throws IOException {
        logger.info("Starting up");
        amqp.connect(ServiceSettings.getAmqpUri());
        final MessageProcessor processor = new MessageProcessor(amqp, cipher, componentResolver);
        amqp.subscribeConsumer(ServiceSettings.getListenMessagesOn(), processor);
        logger.info("Connected to AMQP successfully");
    }
}