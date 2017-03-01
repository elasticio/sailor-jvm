package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import io.elastic.api.Module;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;
import java.util.Map;

public class MessageProcessorImpl implements MessageProcessor {


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageProcessorImpl.class);

    private final EmitterCallbackFactory emitterCallbackFactory;
    private final Step step;

    @Inject
    public MessageProcessorImpl(EmitterCallbackFactory emitterCallbackFactory,
                                @Named(Constants.NAME_STEP_JSON) Step step) {
        this.emitterCallbackFactory = emitterCallbackFactory;
        this.step = step;
    }

    public ExecutionStats processMessage(final Message incomingMessage,
                                         final AMQP.BasicProperties amqpProperties,
                                         final Module module) {

        final ExecutionContext executionContext = new ExecutionContext(
                this.step, incomingMessage, amqpProperties);

        final JsonObject cfg = this.step.getCfg();
        final JsonObject snapshot = this.step.getSnapshot();

        // make data callback
        final CountingCallback dataCallback = emitterCallbackFactory.createDataCallback(executionContext);

        // make error callback
        final CountingCallback errorCallback = emitterCallbackFactory.createErrorCallback(executionContext);

        // make rebound callback
        final CountingCallback reboundCallback = emitterCallbackFactory.createReboundCallback(executionContext);

        // snapshot callback
        final CountingCallback snapshotCallback = emitterCallbackFactory.createSnapshotCallback(executionContext);

        // updateKeys callback
        final EventEmitter.Callback updateKeysCallback
                = emitterCallbackFactory.createUpdateKeysCallback(executionContext);

        // httpReplyCallback callback
        final EventEmitter.Callback httpReplyCallback
                = emitterCallbackFactory.createHttpReplyCallback(executionContext);

        final EventEmitter eventEmitter = new EventEmitter.Builder()
                .onData(dataCallback)
                .onError(errorCallback)
                .onRebound(reboundCallback)
                .onSnapshot(snapshotCallback)
                .onUpdateKeys(updateKeysCallback)
                .onHttpReplyCallback(httpReplyCallback)
                .build();

        final ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage, eventEmitter)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        try {
            module.execute(params);
        } catch (RuntimeException e) {
            logger.error("Component execution failed", e);
            eventEmitter.emitException(e);
        }

        return new ExecutionStats(dataCallback.getCount(), errorCallback.getCount(), reboundCallback.getCount());
    }
}
