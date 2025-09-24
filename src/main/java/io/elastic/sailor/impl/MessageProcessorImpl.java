package io.elastic.sailor.impl;

import com.google.inject.Inject;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import io.elastic.api.Function;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonObject;

public class MessageProcessorImpl implements MessageProcessor {


    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageProcessorImpl.class);

    private final EmitterCallbackFactory emitterCallbackFactory;

    @Inject
    public MessageProcessorImpl(EmitterCallbackFactory emitterCallbackFactory) {
        this.emitterCallbackFactory = emitterCallbackFactory;
    }

    public ExecutionStats processMessage(final ExecutionContext executionContext,
                                         final Function function) {

        final Message incomingMessage = executionContext.getMessage();
        final Step step = executionContext.getStep();
        final JsonObject cfg = step.getCfg();

        final JsonObject snapshot = executionContext.getSnapshot();

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

        final Object messageId = executionContext.getHeaders().get(Constants.AMQP_HEADER_MESSAGE_ID);

        try {
            logger.info("Starting processing of messageId={}", messageId);
            function.execute(params);
            logger.info("Successfully finished processing of messageId={}", messageId);
        } catch (Exception e) {
            logger.error("Component execution failed for messageId={}", messageId, e);
            eventEmitter.emitException(e);
        }

        return new ExecutionStats(dataCallback.getCount(), errorCallback.getCount(), reboundCallback.getCount());
    }
}
