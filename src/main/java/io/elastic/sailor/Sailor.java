package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private Settings settings;
    private AMQPWrapperInterface amqp;
    private ComponentResolver componentResolver;

    public static void main(String[] args) throws IOException {
        Sailor sailor = new Sailor();
        sailor.init(System.getenv());
        sailor.start();
    }

    public void init(Map<String, String> envVars) {
        settings = new Settings(envVars);
        componentResolver = new ComponentResolver(settings.get("COMPONENT_PATH"));
    }

    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    public void start() throws IOException {
        logger.info("Starting up");
        amqp = new AMQPWrapper(settings);
        amqp.connect(settings.get("AMQP_URI"));
        amqp.listenQueue(settings.get("LISTEN_MESSAGES_ON"), new Sailor.Callback(){
            public void receive(Message message, Map<String,Object> headers, Long deliveryTag) {
                processMessage(message, headers, deliveryTag);
            }
        });
        logger.info("Connected to AMQP successfully");
    }

    public interface Callback{
        void receive(Message message, Map<String,Object> headers, Long deliveryTag);
    }

    public void processMessage(final Message incomingMessage, final Map<String,Object> incomingHeaders, final Long deliveryTag){

        String triggerOrAction = settings.getFunction();
        String className = componentResolver.findTriggerOrAction(triggerOrAction);
        JsonObject cfg = settings.getCfg();
        JsonObject snapshot = settings.getSnapshot();

        ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        TaskExecutor executor = new TaskExecutor(className);
        final MessageProcessor processor = new MessageProcessor(
                incomingMessage,
                incomingHeaders,
                deliveryTag,
                amqp, settings, new CipherWrapper()
        );

        // make data callback
        EventEmitter.Callback dataCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processData(obj);
            }
        };

        // make error callback
        EventEmitter.Callback errorCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processError(obj);
            }
        };

        // make rebound callback
        EventEmitter.Callback reboundCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processRebound(obj);
            }
        };

        // snapshot callback
        EventEmitter.Callback snapshotCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processSnapshot(obj);
            }
        };

        // end callback
        EventEmitter.Callback endCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                processor.processEnd(obj);
            }
        };

        executor.onData(dataCallback)
                .onError(errorCallback)
                .onRebound(reboundCallback)
                .onSnapshot(snapshotCallback)
                .onEnd(endCallback);

        executor.execute(params);
    }
}
