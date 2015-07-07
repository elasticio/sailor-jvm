package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
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
        amqp.listenQueue(settings.get("LISTEN_MESSAGES_ON"), settings.get("MESSAGE_CRYPTO_PASSWORD"), new Sailor.Callback(){
            public void receive(Message message, Map<String,Object> headers, Long deliveryTag) {
                processMessage(message, headers, deliveryTag);
            }
        });
        logger.info("Connected to AMQP successfully");
    }

    public interface Callback{
        void receive(Message message, Map<String,Object> headers, Long deliveryTag);
    }

    public void processMessage(final Message incomingMessage, final Map<String,Object> incomingHeaders, Long deliveryTag){

        final Map<String,Object> headers = new HashMap<String, Object>();
        headers.put("execId", incomingHeaders.get("execId"));
        headers.put("taskId", incomingHeaders.get("taskId"));
        headers.put("userId", incomingHeaders.get("userId"));
        headers.put("stepId", settings.getStepId());
        headers.put("compId", settings.getCompId());
        headers.put("function", settings.getTriggerOrAction());
        headers.put("start", System.currentTimeMillis());

        String triggerOrAction = settings.getFunction();
        String className = componentResolver.findTriggerOrAction(triggerOrAction);
        JsonObject cfg = settings.getCfg();
        JsonObject snapshot = settings.getSnapshot();

        ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        TaskExecutor executor = new TaskExecutor(className);

        // make data callback
        EventEmitter.Callback dataCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                Message message = (Message)obj;
                headers.put("end", System.currentTimeMillis());
                amqp.sendData(message.getBody(), headers);
            }
        };

        // make error callback
        EventEmitter.Callback errorCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                Error err = (Error)obj;
                headers.put("end", System.currentTimeMillis());
                amqp.sendError(err, headers, incomingMessage.getBody());
            }
        };

        // make rebound callback
        EventEmitter.Callback reboundCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {
                Error err = (Error)obj;
                headers.put("end", System.currentTimeMillis());
                headers.put("reboundReason", err.message);
                amqp.sendRebound(incomingMessage.getBody(), headers);
            }
        };

        // snapshot callback
        EventEmitter.Callback snapshotCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {

            }
        };

        // end callback
        EventEmitter.Callback endCallback = new EventEmitter.Callback() {
            @Override
            public void receive(Object obj) {

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