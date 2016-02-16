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
    private final Step step;

    @Inject
    public MessageProcessorImpl(ComponentResolver componentResolver,
                                EmitterCallbackFactory emitterCallbackFactory,
                                @Named(Constants.NAME_STEP_JSON) Step step) {
        this.componentResolver = componentResolver;
        this.emitterCallbackFactory = emitterCallbackFactory;
        this.step = step;
    }

    public ExecutionStats processMessage(final Message incomingMessage,
                                         final Map<String, Object> incomingHeaders,
                                         final Long deliveryTag) {

        final ExecutionContext executionContext = new ExecutionContext(
                this.step, incomingMessage, incomingHeaders);

        logger.info("Processing step '{}' of a task", this.step.getId());

        final String triggerOrAction = this.step.getFunction();
        final String className = componentResolver.findTriggerOrAction(triggerOrAction);
        final JsonObject cfg = this.step.getCfg();
        final JsonObject snapshot = this.step.getSnapshot();

        logger.info("Component to be executed: {}", this.step.getCompId());
        logger.info("Trigger/action to be executed: {}", this.step.getFunction());
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
