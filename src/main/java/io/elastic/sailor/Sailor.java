package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.elastic.api.*;
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
    private FunctionBuilder functionBuilder;
    private Step step;
    private ContainerContext containerContext;
    private ApiClient apiClient;
    private boolean isShutdownRequired;
    private AmqpService amqp;
    private ErrorPublisher errorPublisher;

    public static void main(String[] args) throws IOException {
        logger.info("About to init Sailor");
        createAndStartSailor();
    }

    static Sailor createAndStartSailor() throws IOException {

        com.google.inject.Module[] modules = new com.google.inject.Module[] {
                new SailorModule(), new SailorEnvironmentModule()
        };

        Injector injector = Guice.createInjector(modules);

        final Sailor sailor = injector.getInstance(Sailor.class);

        sailor.startOrShutdown(injector);

        return sailor;
    }

    @Inject
    public void setFunctionBuilder(final FunctionBuilder functionBuilder) {
        this.functionBuilder = functionBuilder;
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

    public void startOrShutdown(final Injector injector) {
        if (this.isShutdownRequired) {
            shutdown();
            return;
        }

        start(injector.createChildInjector(new AmqpAwareModule(), new AmqpEnvironmentModule()));
    }

    public void start(final Injector injector) {

        amqp = injector.getInstance(AmqpService.class);
        logger.info("Connecting to AMQP");
        amqp.connectAndSubscribe();

        errorPublisher = injector.getInstance(ErrorPublisher.class);

        try {
            logger.info("Processing flow step: {}", this.step.getId());
            logger.info("Component id to be executed: {}", this.step.getCompId());
            logger.info("Function to be executed: {}", this.step.getFunction());

            final JsonObject cfg = this.step.getCfg();

            final Function function = functionBuilder.build();

            startupModule(function, cfg);

            logger.info("Initializing function for execution");
            final InitParameters initParameters = new InitParameters.Builder()
                    .configuration(cfg)
                    .build();
            function.init(initParameters);

            logger.info("Subscribing to queues");
            amqp.subscribeConsumer(function);
        } catch (Exception e) {
            reportException(e);
        }

        logger.info("Sailor started");
    }

    private void startupModule(final Function function, final JsonObject cfg) {

        if (containerContext.isStartupRequired()) {
            logger.info("Starting up component function");
            final StartupParameters startupParameters = new StartupParameters.Builder()
                    .configuration(cfg)
                    .build();
            JsonObject state = function.startup(startupParameters);

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
        final Function function = functionBuilder.build();

        final JsonObject state = this.apiClient.retrieveStartupState(flowId);

        final ShutdownParameters shutdownParameters = new ShutdownParameters.Builder()
                .configuration(cfg)
                .state(state)
                .build();

        function.shutdown(shutdownParameters);

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

        this.errorPublisher.publish(e, Utils.buildAmqpProperties(headers), null);
    }
}