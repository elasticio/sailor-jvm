package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;

import java.util.HashMap;
import java.util.Map;

public class MessageProcessor {

    // incoming data
    private final Message incomingMessage;
    private final Map<String,Object> incomingHeaders;
    private final long deliveryTag;

    // amqp, cipher, settings
    private final AMQPWrapperInterface amqp;
    private final CipherWrapper cipher;
    private final Settings settings;

    public MessageProcessor(
            final Message incomingMessage,
            final Map<String,Object> incomingHeaders,
            final long deliveryTag,
            AMQPWrapperInterface amqp,
            Settings settings,
            CipherWrapper cipher
    ) {
        this.incomingMessage = incomingMessage;
        this.incomingHeaders = incomingHeaders;
        this.deliveryTag = deliveryTag;
        this.amqp = amqp;
        this.settings = settings;
        this.cipher = cipher;
    }

    private Map<String, Object> makeDefaultHeaders(){
        HashMap<String, Object> headers = new HashMap<String, Object>();
        headers.put("execId", incomingHeaders.get("execId"));
        headers.put("taskId", incomingHeaders.get("taskId"));
        headers.put("userId", incomingHeaders.get("userId"));
        headers.put("stepId", settings.getStepId());
        headers.put("compId", settings.getCompId());
        headers.put("function", settings.getFunction());
        headers.put("start", System.currentTimeMillis());
        return headers;
    }

    private AMQP.BasicProperties makeDefaultOptions(){
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(makeDefaultHeaders())
                .priority(1)// this should equal to mandatory true
                .deliveryMode(2)//TODO: check if flag .mandatory(true) was set
                .build();
    }

    // should send encrypted data to RabbitMQ
    public void processData(Object obj){

        System.out.println("Data received");

        // payload
        Message message = (Message)obj;

        // encrypt
        byte[] encryptedPayload = cipher.encryptMessage(message).getBytes();
        amqp.sendData(encryptedPayload, makeDefaultOptions());
    }

    // should send error to RabbitMQ
    public void processError(Object obj){
        Error err = new Error((Throwable)obj);
        sendError(err);
    }

    private void sendError(Error err) {
        // payload
        JsonObject error = new JsonObject();
        error.addProperty("name", err.name);
        error.addProperty("message", err.message);
        error.addProperty("stack", err.stack);

        JsonObject payload = new JsonObject();
        payload.addProperty("error", cipher.encryptMessageContent(error));
        payload.addProperty("errorInput", cipher.encryptMessage(incomingMessage));

        byte[] errorPayload = payload.toString().getBytes();

        amqp.sendError(errorPayload, makeDefaultOptions());
    }

    // should send snapshot to RabbitMQ
    public void processSnapshot(Object obj){
        JsonObject snapshot = (JsonObject)obj;
        byte[] payload = snapshot.toString().getBytes();
        amqp.sendSnapshot(payload, makeDefaultOptions());
    }

    // should send rebound to RabbitMQ
    public void processRebound(Object obj){

        int reboundIteration = getReboundIteration();

        if (reboundIteration > settings.getInt("REBOUND_LIMIT")) {
            Error err = new Error("Error", "Rebound limit exceeded", Error.getStack(new RuntimeException()));
            sendError(err);
        } else {
            byte[] payload = cipher.encryptMessage(incomingMessage).getBytes();
            Map<String,Object> headers = makeDefaultHeaders();
            headers.put("reboundReason", obj.toString());
            headers.put("reboundIteration", reboundIteration);
            double expiration = getReboundExpiration(reboundIteration);
            amqp.sendRebound(payload, makeReboundOptions(headers, expiration));
        }
    }

    // should ack message
    public void processEnd(Object obj){
        System.out.println("End received");
        amqp.ack(deliveryTag);
    }

    private int getReboundIteration(){
        if (incomingHeaders.get("reboundIteration") != null) {
            try {
                return Integer.parseInt(incomingHeaders.get("reboundIteration").toString()) + 1;
            } catch (Exception e) {
                throw new RuntimeException("Not a number in reboundIteration header: " + incomingHeaders.get("reboundIteration"));
            }
        } else {
            return 1;
        }
    }

    private double getReboundExpiration(int reboundIteration) {
        return Math.pow(2, reboundIteration - 1) * settings.getInt("REBOUND_INITIAL_EXPIRATION");
    }

    private AMQP.BasicProperties makeReboundOptions(Map<String,Object> headers, double expiration){
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .expiration(Double.toString(expiration))
                .headers(headers)
                        //TODO: .mandatory(true)
                .build();
    }
}
