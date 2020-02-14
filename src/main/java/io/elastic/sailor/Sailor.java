package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.elastic.api.InitParameters;
import io.elastic.api.Module;
import io.elastic.api.ShutdownParameters;
import io.elastic.api.StartupParameters;
import io.elastic.sailor.impl.BunyanJsonLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
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
    private boolean isShutdownRequired;

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
        BunyanJsonLayout.containerContext = containerContext;
    }

    @Inject
    public void setApiClient(final ApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Inject
    public void setShutdownRequired(@Named(Constants.ENV_VAR_HOOK_SHUTDOWN) final boolean shutdownRequired) {
        this.isShutdownRequired = shutdownRequired;
    }

    public void startOrShutdown() {
        if (this.isShutdownRequired) {
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
            final InitParameters initParameters = new InitParameters.Builder()
                    .configuration(cfg)
                    .build();
            module.init(initParameters);

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
            final StartupParameters startupParameters = new StartupParameters.Builder()
                    .configuration(cfg)
                    .build();
            JsonObject state = module.startup(startupParameters);

            if (state == null || state.isEmpty()) {
                state = Json.createObjectBuilder().build();
            }

            final String flowId = containerContext.getFlowId();
            try {
                apiClient.storeStartupState(flowId, state);
            } catch (UnexpectedStatusCodeException e) {
                logger.warn("Startup data already exists. Rewriting.");
                apiClient.deleteStartupState(flowId);
                apiClient.storeStartupState(flowId, state);
            }
        }
    }

    public void shutdown() {
        logger.info("Shutting down component");

        final String flowId = containerContext.getFlowId();
        final JsonObject cfg = this.step.getCfg();
        final Module module = moduleBuilder.build();

        final JsonObject state = this.apiClient.retrieveStartupState(flowId);

        final ShutdownParameters shutdownParameters = new ShutdownParameters.Builder()
                .configuration(cfg)
                .state(state)
                .build();

        module.shutdown(shutdownParameters);

        this.apiClient.deleteStartupState(flowId);

        logger.info("Component shut down successfully");
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