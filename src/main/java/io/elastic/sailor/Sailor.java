package io.elastic.sailor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rabbitmq.client.*;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Sailor {

    private static final Logger logger = LoggerFactory.getLogger(Sailor.class);

    private String queueName = "task.worker.queue.origin";
    private ComponentResolver resolver;
    private Channel channel;


    public static void main(String[] args) throws IOException {
        new Sailor().start();
    }

    public void start() throws IOException {
        logger.info("Starting up");

        final Connection connection = amqpConnect("amqp://guest:guest@localhost:5672");

        logger.info("Connected to AMQP successfully");

        channel = connection.createChannel();

        resolver = new ComponentResolver(null); // @TODO pass here component path

        channel.basicConsume(queueName, false, new MessageConsumer(channel, this));
    }

    private static Connection amqpConnect(final String connectionUri) {
        ConnectionFactory factory = new ConnectionFactory();
        try {
            factory.setUri(connectionUri);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        try {
            return factory.newConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonObject getTask(){
        String task = System.getenv("MESSAGE_CRYPTO_PASSWORD");
        return new JsonParser().parse(task).getAsJsonObject();
    }

    public static String getStepId(){
        return System.getenv("STEP_ID");
    }

    public static JsonObject getStepCfg(JsonObject task, String stepId){
        return task.get("data").getAsJsonObject().get(stepId).getAsJsonObject();
    }

    public static JsonObject getStepSnapshot(JsonObject task, String stepId){
        return task.get("data").getAsJsonObject().get(stepId).getAsJsonObject();
    }

    public static JsonObject getStepInfo(JsonObject task, String stepId){
        JsonArray nodes = task.get("recipe").getAsJsonObject().get("nodes").getAsJsonArray();
        for (JsonElement node : nodes) {
            if (node.getAsJsonObject().get("id").getAsString().equals(stepId)) {
                return node.getAsJsonObject();
            }
        }
        return null;
    }

    public void processMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body){

        JsonObject task = getTask();
        String stepId = getStepId();


        JsonObject stepInfo = getStepInfo(task, stepId);
        String stepFunction = stepInfo.get("function").getAsString();
        String className = resolver.findTriggerOrAction(stepFunction);

        Message message = createMessage(body);
        JsonObject cfg = getStepCfg(task, stepId);
        JsonObject snapshot = getStepSnapshot(task, stepId);

        ExecutionParameters params = new ExecutionParameters.Builder(message)
                .configuration(cfg)
                .snapshot(snapshot)
                .build();

        TaskExecutor executor = new TaskExecutor(className);
        executor.onData(getDataCallback());
        executor.onError(getErrorCallback());
        executor.onRebound(getReboundCallback());
        executor.onSnapshot(getSnapshotCallback());
        executor.execute(params);
    }

    private static Message createMessage(byte[] body) {
        return new Message.Builder().build();
    }

    // data
    private EventEmitter.Callback getDataCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                System.out.println("newDataCallback" + data.toString());
                // @TODO process data and send to channel
            }
        };
    }

    private EventEmitter.Callback getErrorCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object err) {
                System.out.println("newErrorCallback" + err.toString());
                // @TODO process error and send to channel
            }
        };
    }

    private EventEmitter.Callback getReboundCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                System.out.println("newReboundCallback" + data.toString());
                // @TODO process rebound and send to channel
            }
        };
    }

    private EventEmitter.Callback getSnapshotCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                System.out.println("newSnapshotCallback" + data.toString());
                // @TODO process snapshot and send to channel
            }
        };
    }

    private EventEmitter.Callback getEndCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object err) {
                System.out.println("newEndCallback");
                // @TODO process end and send to channel
            }
        };
    }
}
