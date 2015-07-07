package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.rabbitmq.client.*;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class AMQPWrapper implements AMQPWrapperInterface {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(AMQPWrapper.class);

    private final Settings settings;
    private Connection amqp;
    private Channel subscribeChannel;
    private Channel publishChannel;

    public AMQPWrapper(Settings settings) {
        this.settings = settings;
    }

    public void connect(String link) {
        openConnection(link);
        openPublishChannel();
        openSubscribeChannel();
    }

    public void disconnect() {
        logger.info("About to disconnect from AMQP");
        try {
            subscribeChannel.close();
        } catch (IOException e) {
            logger.info("Subscription channel is already closed: " + e);
        }
        try {
            publishChannel.close();
        } catch (IOException e) {
            logger.info("Publish channel is already closed: " + e);
        }
        try {
            amqp.close();
        } catch (IOException e) {
            logger.info("AMQP connection is already closed: " + e);
        }
        logger.info("Successfully disconnected from AMQP");
    }

    public void listenQueue(String queueName, String cipherKey, Sailor.Callback callback) {
        try {
            MessageConsumer consumer = new MessageConsumer(subscribeChannel, cipherKey, callback);
            subscribeChannel.basicConsume(queueName, consumer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void ack(Long deliveryTag) {
        try {
            logger.info(String.format("Message #%s ack", deliveryTag));
            subscribeChannel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void reject(Long deliveryTag) {
        try {
            logger.info(String.format("Message #%s reject", deliveryTag));
            subscribeChannel.basicReject(deliveryTag, false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void sendToExchange(String exchangeName, String routingKey, byte[] payload, AMQP.BasicProperties options) {
        logger.info(String.format("Pushing to exchange=%s, routingKey=%s, data=%s, options=%s",
                exchangeName, routingKey, new String(payload), options));
        try {
            publishChannel.basicPublish(exchangeName, routingKey, options, payload);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Failed to publish message to exchange %s, %s", exchangeName, e));
        }
    }

    public void sendData(JsonObject data, final Map<String,Object> headers) {
        AMQP.BasicProperties options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                        //TODO: .mandatory(true)
                .build();
        try {
            byte[] encryptedData = new CipherWrapper().encryptMessageContent(data).getBytes();
            sendToExchange(
                    settings.get("PUBLISH_MESSAGES_TO"),
                    settings.get("DATA_ROUTING_KEY"),
                    encryptedData,
                    options
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendError(Error err, final Map<String,Object> headers, JsonObject originalMessage) {
        AMQP.BasicProperties options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                        //TODO: .mandatory(true)
                .build();

        JsonObject errorJson = new JsonObject();
        errorJson.addProperty("name", err.name);
        errorJson.addProperty("message", err.message);
        errorJson.addProperty("stack", err.stack);

        JsonObject payload = new JsonObject();
        payload.add("error", errorJson);

        /*if (!originalMessageContent.isEmpty()) {
            payload.addProperty("errorInput", originalMessageContent);
        }*/
        byte[] errorPayload = payload.toString().getBytes(); // TODO: was stringify() - check
        sendToExchange(
                settings.get("PUBLISH_MESSAGES_TO"),
                settings.get("ERROR_ROUTING_KEY"),
                errorPayload,
                options
        );
    }

    public void sendRebound(JsonObject originalMessage, Map<String, Object> headers) {
        logger.info("Rebound message: %s", originalMessage.toString());
        int reboundIteration = getReboundIteration(originalMessage.get("properties").getAsJsonObject().get("headers").getAsJsonObject().get("reboundIteration").getAsInt());

        if (reboundIteration > Integer.parseInt(settings.get("REBOUND_LIMIT"))) {
            sendError(new Error("error", "Rebound limit exceeded", "stacktrace"), headers, originalMessage);
        } else {
            Map<String, Object> headersCopy = new HashMap<String, Object>();
            headersCopy.putAll(headers);
            AMQP.BasicProperties options = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .contentEncoding("utf8")
                    .headers(headersCopy)
                    .expiration(String.valueOf(getExpiration(reboundIteration)))
                            //TODO: .mandatory(true)
                    .build();

            options.getHeaders().put("reboundIteration", reboundIteration);

            sendToExchange(
                    settings.get("PUBLISH_MESSAGES_TO"),
                    settings.get("REBOUND_ROUTING_KEY"),
                    originalMessage.get("content").toString().getBytes(),
                    options
            );

        }
    }

    private double getExpiration(int reboundIteration) {
        return Math.pow(2, reboundIteration - 1) * settings.getInt("REBOUND_INITIAL_EXPIRATION");
    }

    private AMQPWrapper openConnection(String uri) {
        try {
            if (amqp == null) {
                ConnectionFactory factory = new ConnectionFactory();
                factory.setUri(new URI(uri));
                amqp = factory.newConnection();
                logger.info("Connected to AMQP");
            }
            return this;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private AMQPWrapper openPublishChannel() {
        try {
            if (publishChannel == null) {
                publishChannel = amqp.createChannel();
                logger.info("Opened publish channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private AMQPWrapper openSubscribeChannel() {
        try {
            if (subscribeChannel == null) {
                subscribeChannel = amqp.createChannel();
                logger.info("Opened subscribe channel");
            }
            return this;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int getReboundIteration(int previousIteration) {
        return previousIteration + 1;
    }

    @Override
    protected void finalize() throws Throwable {
        disconnect();
        super.finalize();
    }
}