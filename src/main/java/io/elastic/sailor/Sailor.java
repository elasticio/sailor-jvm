package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.elastic.api.Module;
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
    private ApiClient apiClient;

    public static void main(String[] args) throws IOException {
        createAndStartSailor();
    }

    static Sailor createAndStartSailor() throws IOException {
        Injector injector = Guice.createInjector(
                new SailorModule(), new SailorEnvironmentModule());

        final Sailor sailor = injector.getInstance(Sailor.class);

        sailor.startOrShutdown();

        return sailor;
    }

    @Inject
    public void setAMQP(final AmqpService amqp) {
        this.amqp = amqp;
    }

    @Inject
    public void setModuleBuilder(final ModuleBuilder moduleBuilder) {
        this.moduleBuilder = moduleBuilder;
    }

    @Inject
    public void setStep(final @Named(Constants.NAME_STEP_JSON) Step step) {
        this.step = step;
    }

    @Inject
    public void setContainerContext(final ContainerContext containerContext) {
        this.containerContext = containerContext;
    }

    @Inject
    public void setApiClient(final ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    public void startOrShutdown(){
        if (containerContext.isShutdownRequired()) {
            shutdown();
            return;
        }

        start();
    }

    public void start() {

        logger.info("Connecting to AMQP");
        amqp.connect();

        try {
            logger.info("Processing flow step: {}", this.step.getId());
            logger.info("Component id to be executed: {}", this.step.getCompId());
            logger.info("Module to be executed: {}", this.step.getFunction());

            final JsonObject cfg = this.step.getCfg();

            final Module module = moduleBuilder.build();

            startupModule(module, cfg);

            logger.info("Initializing module for execution");
            module.init(cfg);

            logger.info("Subscribing to queues");
            amqp.subscribeConsumer(module);
        } catch (Exception e) {
            reportException(e);
        }

        logger.info("Sailor started");
    }

    private void startupModule(final Module module, final JsonObject cfg) {

        if (containerContext.isStartupRequired()) {
            logger.info("Starting up component module");
            final JsonObject state = module.startup(cfg);

            if (state != null && !state.isEmpty()) {
                apiClient.storeStartupState(containerContext.getFlowId(), state);
            }
        }
    }

    public void shutdown() {
        logger.info("Shutting down component module");

        final String flowId = containerContext.getFlowId();
        final JsonObject cfg = this.step.getCfg();
        final Module module = moduleBuilder.build();

        final JsonObject state = this.apiClient.retrieveStartupState(flowId);

        module.shutdown(cfg, state);

        this.apiClient.deleteStartupState(flowId);

        logger.info("Component module shut down successfully");
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