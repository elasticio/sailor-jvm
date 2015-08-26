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
    private final JsonObject task;
    private final String stepId;

    @Inject
    public MessageProcessorImpl(ComponentResolver componentResolver,
                                EmitterCallbackFactory emitterCallbackFactory,
                                @Named("TaskJson") JsonObject task,
                                @Named(Constants.ENV_VAR_STEP_ID) String stepId) {
        this.componentResolver = componentResolver;
        this.emitterCallbackFactory = emitterCallbackFactory;
        this.task = task;
        this.stepId = stepId;
    }

    public void processMessage(final Message incomingMessage,
                               final Map<String, Object> incomingHeaders,
                               final Long deliveryTag) {

        final ExecutionContext executionContext = new ExecutionContext(
                this.stepId, this.task, incomingMessage, incomingHeaders);

        final String triggerOrAction = executionContext.getFunction();
        final String className = componentResolver.findTriggerOrAction(triggerOrAction);
        final JsonObject cfg = executionContext.getCfg();
        final JsonObject snapshot = executionContext.getSnapshot();

        logger.info("About to execute {}", className);

        final ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        // make data callback
        EventEmitter.Callback dataCallback = emitterCallbackFactory.createDataCallback(executionContext);

        // make error callback
        EventEmitter.Callback errorCallback = emitterCallbackFactory.createErrorCallback(executionContext);

        // make rebound callback
        EventEmitter.Callback reboundCallback = emitterCallbackFactory.createReboundCallback(executionContext);

        // snapshot callback
        EventEmitter.Callback snapshotCallback = emitterCallbackFactory.createSnapshotCallback(executionContext);

        final EventEmitter eventEmitter = new EventEmitter.Builder()
                .onData(dataCallback)
                .onError(errorCallback)
                .onRebound(reboundCallback)
                .onSnapshot(snapshotCallback)
                .build();

        final Executor executor = new Executor(className, eventEmitter);

        executor.execute(params);


        //TODO:processor.processEnd();
    }
}
