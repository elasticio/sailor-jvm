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
    private CipherWrapper cipher;

    public static void main(String[] args) throws IOException {

        Map<String, String> envVars  = new HashMap<String, String>();
        envVars.put("TASK", "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");
        envVars.put("STEP_ID", "step_1");
        envVars.put("AMQP_URI", "amqp://guest:guest@127.0.0.1:5672");
        envVars.put("LISTEN_MESSAGES_ON", "javasailor:test_exec:step_1:messages");
        envVars.put("PUBLISH_MESSAGES_TO", "javasailor_exchange");
        envVars.put("DATA_ROUTING_KEY", "javasailor.test_exec.step_1.message");
        envVars.put("ERROR_ROUTING_KEY", "javasailor.test_exec.step_1.error");
        envVars.put("SNAPSHOT_ROUTING_KEY", "javasailor.test_exec.step_1.snapshot");
        envVars.put("REBOUND_ROUTING_KEY", "javasailor.test_exec.step_1.rebound");
        envVars.put("SHAPSHOT_ROUTING_KEY", "javasailor.test_exec.step_1.rebound");
        envVars.put("COMPONENT_PATH", "src/test/java/groovy/io/elastic/sailor/component");

        Sailor sailor = new Sailor();
        sailor.init(envVars);
        sailor.start();
    }

    public void setSettings(Settings settings){
        this.settings = settings;
    }

    public void setAMQP(AMQPWrapperInterface amqp) {
        this.amqp = amqp;
    }

    public void setCipher(CipherWrapper cipher) {
        this.cipher = cipher;
    }

    public void init(Map<String, String> envVars) {
        settings = new Settings(envVars);
        componentResolver = new ComponentResolver(settings.get("COMPONENT_PATH"));
        cipher = new CipherWrapper(settings.get("MESSAGE_CRYPTO_PASSWORD"), settings.get("MESSAGE_CRYPTO_IV"));
    }

    public void start() throws IOException {
        logger.info("Starting up");
        amqp = new AMQPWrapper(settings);
        amqp.connect(settings.get("AMQP_URI"));
        amqp.listenQueue(settings.get("LISTEN_MESSAGES_ON"), cipher, getMessageCallback());
        logger.info("Connected to AMQP successfully");
    }

    public interface Callback{
        void receive(Message message, Map<String,Object> headers, Long deliveryTag);
    }

    private Sailor.Callback getMessageCallback(){
        return new Sailor.Callback(){
            public void receive(Message message, Map<String,Object> headers, Long deliveryTag) {
                processMessage(message, headers, deliveryTag);
            }
        };
    }

    public MessageProcessor getMessageProcessor(final Message incomingMessage, final Map<String,Object> incomingHeaders, final Long deliveryTag) {
        return new MessageProcessor(
            incomingMessage,
            incomingHeaders,
            deliveryTag,
            amqp,
            settings,
            cipher
        );
    }

    public void processMessage(final Message incomingMessage, final Map<String,Object> incomingHeaders, final Long deliveryTag){

        final String triggerOrAction = settings.getFunction();
        final String className = componentResolver.findTriggerOrAction(triggerOrAction);
        final JsonObject cfg = settings.getCfg();
        final JsonObject snapshot = settings.getSnapshot();

        final ExecutionParameters params = new ExecutionParameters.Builder(incomingMessage)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        final TaskExecutor executor = new TaskExecutor(className);
        final MessageProcessor processor = getMessageProcessor(incomingMessage, incomingHeaders, deliveryTag);

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
