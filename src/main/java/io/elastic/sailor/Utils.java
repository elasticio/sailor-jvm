package io.elastic.sailor;

import com.rabbitmq.client.AMQP;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.impl.MessageEncoding;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
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
        JsonString id = payload.getJsonString(Message.PROPERTY_ID);
        JsonObject headers = payload.getJsonObject(Message.PROPERTY_HEADERS);
        JsonObject body = payload.getJsonObject(Message.PROPERTY_BODY);
        JsonObject attachments = payload.getJsonObject(Message.PROPERTY_ATTACHMENTS);
        JsonObject passthrough = payload.getJsonObject(Message.PROPERTY_PASSTHROUGH);

        if (headers == null) {
            headers = Json.createObjectBuilder().build();
        }

        if (body == null) {
            body = Json.createObjectBuilder().build();
        }

        if (attachments == null) {
            attachments = Json.createObjectBuilder().build();
        }

        if (passthrough == null) {
            passthrough = Json.createObjectBuilder().build();
        }

        final Message.Builder builder = new Message.Builder()
                .headers(headers)
                .body(body)
                .attachments(attachments)
                .passthrough(passthrough);

        if (id != null) {
            builder.id(UUID.fromString(id.getString()));
        }

        return builder.build();
    }


    public static JsonObject pick(final JsonObject obj, String... properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Properties must not be null");
        }
        final List<String> propertiesList = Arrays.asList(properties);

        final JsonObjectBuilder result = Json.createObjectBuilder();
        obj.entrySet()
                .stream()
                .filter(s -> propertiesList.contains(s.getKey()))
                .forEach(s -> result.add(s.getKey(), s.getValue()));
        return result.build();
    }

    public static String getThreadId(final AMQP.BasicProperties properties) {
        final Map<String, Object> headers = properties.getHeaders();
        final Object threadId = headers.get(Constants.AMQP_HEADER_THREAD_ID);
        final Object traceId = headers.get(Constants.AMQP_META_HEADER_TRACE_ID);

        if (threadId != null) {
            return threadId.toString();
        }

        if (traceId != null) {
            return traceId.toString();
        }

        return "unknown";
    }

    public static JsonObjectBuilder copy(final JsonObject object) {

        final JsonObjectBuilder result = Json.createObjectBuilder();

        object.entrySet()
                .stream()
                .forEach(s -> result.add(s.getKey(), s.getValue()));

        return result;
    }

    public static JsonObject omit(final JsonObject obj, String... properties) {
        if (properties == null) {
            throw new IllegalArgumentException("Properties must not be null");
        }
        final List<String> propertiesList = Arrays.asList(properties);

        final JsonObjectBuilder result = Json.createObjectBuilder();
        obj.entrySet()
                .stream()
                .filter(s -> !propertiesList.contains(s.getKey()))
                .forEach(s -> result.add(s.getKey(), s.getValue()));
        return result.build();
    }

    public static String getStackTrace(Throwable e) {
        final StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));

        return writer.toString();
    }

    public static MessageEncoding getMessageEncoding(final AMQP.BasicProperties properties) {

        final int protocolVersion = (int) properties.getHeaders().getOrDefault(
                Constants.AMQP_HEADER_PROTOCOL_VERSION, MessageEncoding.BASE64.protocolVersion);

        return MessageEncoding.fromProtocolVersion(protocolVersion);
    }
}