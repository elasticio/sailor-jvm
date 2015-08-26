package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Executor;
import io.elastic.api.Message;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MessageProcessor {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageProcessor.class);

    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;
    private ComponentResolver componentResolver;

    public MessageProcessor(AMQPWrapperInterface amqp, CipherWrapper cipher, ComponentResolver componentResolver) {
        this.amqp = amqp;
        this.cipher = cipher;
        this.componentResolver = componentResolver;
    }

    public void processMessage(final Message incomingMessage,
                               final Map<String, Object> incomingHeaders,
                               final Long deliveryTag) {

        final ExecutionContext executionContext = new ExecutionContext(incomingMessage, incomingHeaders);
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
        EventEmitter.Callback dataCallback = new DataCallback(executionContext, amqp, cipher);

        // make error callback
        EventEmitter.Callback errorCallback = new ErrorCallback(executionContext, amqp, cipher);

        // make rebound callback
        EventEmitter.Callback reboundCallback = new ReboundCallback(executionContext, amqp, cipher);

        // snapshot callback
        EventEmitter.Callback snapshotCallback = new SnapshotCallback(executionContext, amqp);

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
