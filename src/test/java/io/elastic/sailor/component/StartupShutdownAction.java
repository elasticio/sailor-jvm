package io.elastic.sailor.component;

import com.rabbitmq.client.AMQP;
import io.elastic.api.*;
import io.elastic.sailor.Constants;
import io.elastic.sailor.impl.AmqpServiceImpl;
import io.elastic.sailor.impl.CryptoServiceImpl;
import io.elastic.sailor.impl.HttpUtils;
import io.elastic.sailor.impl.MessagePublisherImpl;
import org.apache.http.impl.client.CloseableHttpClient;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import java.util.Collections;

public class StartupShutdownAction implements Function {

    public static final String SUBSCRIPTION_ID = "my_startup_subscription_123456";

    private JsonObjectBuilder builder;

    @Override
    public JsonObject startup(final StartupParameters parameters) {
        builder = Json.createObjectBuilder()
                .add("startup", parameters.getConfiguration());

        return Json.createObjectBuilder()
                .add("subscriptionId", SUBSCRIPTION_ID)
                .build();
    }

    public void execute(final ExecutionParameters parameters) {
        final JsonObject body = Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .add("startupAndShutdown", builder.build())
                .build();


        final Message msg = new Message.Builder().body(body).build();

        parameters.getEventEmitter().emitData(msg);
    }

    @Override
    public void shutdown(final ShutdownParameters parameters) {
        final JsonObject configuration = parameters.getConfiguration();

        final CryptoServiceImpl cipher = new CryptoServiceImpl(
                configuration.getString(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD),
                configuration.getString(Constants.ENV_VAR_MESSAGE_CRYPTO_IV));

        final CloseableHttpClient httpClient = HttpUtils.createHttpClient(0);
        final AmqpServiceImpl amqp = new AmqpServiceImpl(cipher, httpClient);
        final String publishExchangeName = configuration.getString(Constants.ENV_VAR_PUBLISH_MESSAGES_TO);
        final String dataRoutingKey = configuration.getString(Constants.ENV_VAR_DATA_ROUTING_KEY);

        final MessagePublisherImpl publisher = new MessagePublisherImpl(
                publishExchangeName, Integer.MAX_VALUE, 0,0, true, true, amqp);

        amqp.setMessagePublisher(publisher);
        amqp.setAmqpUri(configuration.getString(Constants.ENV_VAR_AMQP_URI));
        amqp.setPrefetchCount(1);
        amqp.setThreadPoolSize(1);
        amqp.connectAndSubscribe();

        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(Collections.emptyMap())
                .priority(1)
                .deliveryMode(2)
                .build();

        final JsonObject payload = Json.createObjectBuilder()
                .add("shutdownSignal", "1")
                .add("configuration", configuration)
                .build();

        publisher.publish(dataRoutingKey, JSON.stringify(payload).getBytes(), properties);

        try {
            httpClient.close();
        } catch (java.io.IOException e) {
            // ignore
        }
    }
}