package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.JSON;
import io.elastic.api.Message;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.util.Map;
import java.util.UUID;

public class Utils {

    public static boolean isJsonObject(final String input) {
        if (input == null) {
            return false;
        }

        try {
            JSON.parseObject(input);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getEnvVar(final String key) {
        final String value = getOptionalEnvVar(key);

        if (value == null) {
            throw new IllegalStateException(
                    String.format("Env var '%s' is required", key));
        }

        return value;
    }

    public static String getOptionalEnvVar(final String key) {
        String value = System.getenv(key);

        if (value == null) {
            value = System.getProperty(key);
        }

        return value;
    }

    public static AMQP.BasicProperties buildAmqpProperties(final Map<String, Object> headers) {
        return createDefaultAmqpPropertiesBuilder(headers).build();
    }

    private static AMQP.BasicProperties.Builder createDefaultAmqpPropertiesBuilder(final Map<String, Object> headers) {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .priority(1)// this should equal to mandatory true
                .deliveryMode(2);
    }



    public static Message createMessage(final JsonObject payload) {
        JsonString id = payload.getJsonString(Constants.MESSAGE_PROPERTY_ID);
        JsonObject headers = payload.getJsonObject(Constants.MESSAGE_PROPERTY_HEADERS);
        JsonObject body = payload.getJsonObject(Constants.MESSAGE_PROPERTY_BODY);
        JsonObject attachments = payload.getJsonObject(Constants.MESSAGE_PROPERTY_ATTACHMENTS);

        if (headers == null) {
            headers = Json.createObjectBuilder().build();
        }

        if (body == null) {
            body = Json.createObjectBuilder().build();
        }

        if (attachments == null) {
            attachments = Json.createObjectBuilder().build();
        }

        final Message.Builder builder = new Message.Builder()
                .headers(headers)
                .body(body)
                .attachments(attachments);

        if (id != null) {
            builder.id(UUID.fromString(id.getString()));
        }

        return builder.build();
    }

}