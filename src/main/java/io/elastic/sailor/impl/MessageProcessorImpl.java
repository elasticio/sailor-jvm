package io.elastic.sailor.impl;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Executor;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MessageProcessorImpl implements MessageProcessor {


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageProcessorImpl.class);

    private final ComponentResolver componentResolver;
    private final EmitterCallbackFactory emitterCallbackFactory;
    private final JsonObject step;
    private final String stepId;

    @Inject
    public MessageProcessorImpl(ComponentResolver componentResolver,
                                EmitterCallbackFactory emitterCallbackFactory,
                                @Named(Constants.NAME_STEP_JSON) JsonObject step,
                                @Named(Constants.ENV_VAR_STEP_ID) String stepId) {
        this.componentResolver = componentResolver;
        this.emitterCallbackFactory = emitterCallbackFactory;
        this.step = step;
        this.stepId = stepId;
    }

    public ExecutionStats processMessage(final Message incomingMessage,
                                         final Map<String, Object> incomingHeaders,
                                         final Long deliveryTag) {

        final ExecutionContext executionContext = new ExecutionContext(
                this.stepId, this.step, incomingMessage, incomingHeaders);

        logger.info("Processing step '{}' of a task", executionContext.getStepId());

        final String triggerOrAction = executionContext.getFunction();
        final String className = componentResolver.findTriggerOrAction(triggerOrAction);
        final JsonObject cfg = executionContext.getCfg();
        final JsonObject snapshot = executionContext.getSnapshot();

        logger.info("Component to be executed: {}", executionContext.getCompId());
        logger.info("Trigger/action to be executed: {}", executionContext.getFunction());
        logger.info("Component Java class to be instantiated: {}", className);

        final ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        // make data callback
        CountingCallback dataCallback = emitterCallbackFactory.createDataCallback(executionContext);

        // make error callback
        CountingCallback errorCallback = emitterCallbackFactory.createErrorCallback(executionContext);

        // make rebound callback
        CountingCallback reboundCallback = emitterCallbackFactory.createReboundCallback(executionContext);

        // snapshot callback
        CountingCallback snapshotCallback = emitterCallbackFactory.createSnapshotCallback(executionContext);

        final EventEmitter eventEmitter = new EventEmitter.Builder()
                .onData(dataCallback)
                .onError(errorCallback)
                .onRebound(reboundCallback)
                .onSnapshot(snapshotCallback)
                .build();

        final Executor executor = new Executor(className, eventEmitter);

        executor.execute(params);

        return new ExecutionStats(dataCallback.getCount(), errorCallback.getCount(), reboundCallback.getCount());
    }
}
