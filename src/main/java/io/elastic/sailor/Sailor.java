package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.name.Named;
import io.elastic.api.Function;
import io.elastic.api.InitParameters;
import io.elastic.api.ShutdownParameters;
import io.elastic.api.StartupParameters;
import io.elastic.sailor.impl.BunyanJsonLayout;
import io.elastic.sailor.impl.GracefulShutdownHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);
    private Function function;
    private Step step;
    private ContainerContext containerContext;
    private ApiClient apiClient;
    private boolean isShutdownRequired;
    private AmqpService amqp;
    private ErrorPublisher errorPublisher;
    public static GracefulShutdownHandler gracefulShutdownHandler;

    public static void main(String[] args) {
        logger.info("About to init Sailor");
        createAndStartSailor();
    }

    static Sailor createAndStartSailor() {

        com.google.inject.Module[] modules = new com.google.inject.Module[]{
                new SailorModule(), new SailorEnvironmentModule()
        };

        Injector injector = Guice.createInjector(modules);

        final Sailor sailor = injector.getInstance(Sailor.class);

        sailor.startOrShutdown(injector);

        return sailor;
    }

    @Inject
    public void setFunction(@Named(Constants.NAME_FUNCTION_OBJECT) final Function function) {
        this.function = function;
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

        final Injector childInjector = injector.createChildInjector(new AmqpAwareModule(), new AmqpEnvironmentModule());

        start(childInjector);
    }

    public void start(final Injector injector) {

        amqp = injector.getInstance(AmqpService.class);
        logger.info("Connecting to AMQP");
        amqp.connect();
        amqp.createSubscribeChannel();

        errorPublisher = injector.getInstance(ErrorPublisher.class);

        Sailor.gracefulShutdownHandler = new GracefulShutdownHandler(amqp);

        try {
            logger.info("Processing flow step: {}", this.step.getId());
            logger.info("Component id to be executed: {}", this.step.getCompId());
            logger.info("Function to be executed: {}", this.step.getFunction());

            final JsonObject cfg = this.step.getCfg();

            startupModule(cfg);

            logger.info("Initializing function for execution");
            final InitParameters initParameters = new InitParameters.Builder()
                    .configuration(cfg)
                    .build();
            function.init(initParameters);

            logger.info("Subscribing to queues");
            amqp.subscribeConsumer();
        } catch (Exception e) {
            reportException(e);
        }

        logger.info("Sailor started");
    }

    private void startupModule(final JsonObject cfg) {

        if (containerContext.isStartupRequired()) {
            logger.info("Starting up component function");
            final StartupParameters startupParameters = new StartupParameters.Builder()
                    .configuration(cfg)
                    .build();
            JsonObject state = this.function.startup(startupParameters);

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

        final JsonObject state = this.apiClient.retrieveStartupState(flowId);

        final ShutdownParameters shutdownParameters = new ShutdownParameters.Builder()
                .configuration(cfg)
                .state(state)
                .build();

        this.function.shutdown(shutdownParameters);

        this.apiClient.deleteStartupState(flowId);

        logger.info("Component shut down successfully");
    }

    private void reportException(final Exception e) {
        final Map<String, Object> headers = new HashMap<String, Object>();
        headers.put(Constants.AMQP_HEADER_CONTAINER_ID, containerContext.getContainerId());
        headers.put(Constants.AMQP_HEADER_WORKSPACE_ID, containerContext.getWorkspaceId());
        headers.put(Constants.AMQP_HEADER_EXEC_ID, containerContext.getExecId());
        headers.put(Constants.AMQP_HEADER_TASK_ID, containerContext.getFlowId());
        headers.put(Constants.AMQP_HEADER_USER_ID, containerContext.getUserId());
        headers.put(Constants.AMQP_HEADER_STEP_ID, containerContext.getStepId());
        headers.put(Constants.AMQP_HEADER_COMPONENT_ID, containerContext.getCompId());
        headers.put(Constants.AMQP_HEADER_FUNCTION, containerContext.getFunction());

        this.errorPublisher.publish(e, Utils.buildAmqpProperties(headers), null);
    }
}