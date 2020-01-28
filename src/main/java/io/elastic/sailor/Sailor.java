package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.elastic.api.Module;
import io.elastic.sailor.impl.BunyanJsonLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private AmqpService amqp;
    private ModuleBuilder moduleBuilder;
    private Step step;
    private ContainerContext containerContext;

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
    public void setAMQP(AmqpService amqp) {
        this.amqp = amqp;
    }

    @Inject
    public void setModuleBuilder(ModuleBuilder moduleBuilder) {
        this.moduleBuilder = moduleBuilder;
    }

    @Inject
    public void setStep(@Named(Constants.NAME_STEP_JSON) Step step) {
        this.step = step;
    }

    @Inject
    public void setContainerContext(ContainerContext containerContext) {
        this.containerContext = containerContext;
        BunyanJsonLayout.containerContext = containerContext;
    }

    public void start() throws IOException {

        logger.info("Connecting to AMQP");
        amqp.connect();

        try {
            logger.info("Processing flow step: {}", this.step.getId());
            logger.info("Component id to be executed: {}", this.step.getCompId());
            logger.info("Module to be executed: {}", this.step.getFunction());

            final JsonObject cfg = this.step.getCfg();

            final Module module = moduleBuilder.build();

            if (containerContext.isShutdownRequired()) {
                logger.info("Shutdown hook called");
                module.shutdown(cfg);
                return;
            }

            if (containerContext.isStartupRequired()) {
                logger.info("Starting up component");
                module.startup(cfg);
            }

            logger.info("Initializing module for execution");
            module.init(cfg);

            logger.info("Subscribing to queues");
            amqp.subscribeConsumer(module);
        } catch (Exception e) {
            reportException(e);
        }
    }

    private void reportException(final Exception e) {
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put("execId", containerContext.getExecId());
        headers.put("taskId", containerContext.getFlowId());
        headers.put("userId", containerContext.getUserId());
        headers.put("stepId", containerContext.getStepId());
        headers.put("compId", containerContext.getCompId());

        amqp.sendError(e, Utils.buildAmqpProperties(headers), null);
    }
}