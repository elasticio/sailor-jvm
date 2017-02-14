package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.elastic.api.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private AMQPWrapperInterface amqp;
    private ComponentBuilder componentBuilder;
    private Step step;

    public static void main(String[] args) throws IOException {
        createAndStartSailor();
    }

    static Sailor createAndStartSailor() throws IOException {
        Injector injector = Guice.createInjector(
                new SailorModule(), new SailorEnvironmentModule());

        final Sailor sailor = injector.getInstance(Sailor.class);

        sailor.start();

        logger.info("Sailor started");

        return sailor;
    }

    @Inject
    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    @Inject
    public void setComponentBuilder(ComponentBuilder componentBuilder) {
        this.componentBuilder = componentBuilder;
    }

    @Inject
    public void setStep(@Named(Constants.NAME_STEP_JSON) Step step) {
        this.step = step;
    }

    public void start() throws IOException {

        logger.info("Connecting to AMQP");
        amqp.connect();

        try {
            logger.info("Processing flow step: {}", this.step.getId());
            logger.info("Component id to be executed: {}", this.step.getCompId());
            logger.info("Trigger/action to be executed: {}", this.step.getFunction());

            final JsonObject cfg = this.step.getCfg();

            final Component component = componentBuilder.build();

            logger.info("Initializing component for execution");
            component.init(cfg);

            logger.info("Subscribing to queues");
            amqp.subscribeConsumer(component);
        } catch (Exception e) {
            reportException(e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                logger.info("Shutdown hook called");
            }
        });
    }

    private void reportException(final Exception e) {
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("stepId", this.step.getId());
        headers.put("compId", this.step.getCompId());

        amqp.sendError(e, Utils.buildAmqpProperties(headers), null);
    }
}