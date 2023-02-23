package io.elastic.api;


import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.UUID;

/**
 * Message to be processed by a {@link Function}. A message may have a body,
 * which represents a message's payload to be processed, and multiple attachments.
 * Both body and attachments are {@link JsonObject}s.
 *
 * <p>
 *
 * A {@link Function} may retrieve a value from {@link Message}'s body by a name,
 * as shown in the following example.
 *
 * <pre>
 * {@code
 *    JsonArray orders = message.getBody().getJsonArray("orders");
 * }
 * </pre>
 *
 * <p>
 *
 * A message is build using {@link Builder}, as shown in the following example.
 *
 * <pre>
 * {@code
 *    JsonArray orders = JSON.parseArray(response.getOrders());
 *
 *    JsonObject body = Json.createObjectBuilder()
 *            .add("orders", orders)
 *            .build();
 *
 *    Message message = new Message.Builder().body(body).build();
 * }
 * </pre>
 */
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String PROPERTY_ID           = "id";
    public static final String PROPERTY_ATTACHMENTS  = "attachments";
    public static final String PROPERTY_BODY         = "body";
    public static final String PROPERTY_HEADERS      = "headers";
    public static final String PROPERTY_METHOD       = "method";
    public static final String PROPERTY_ORIGINAL_URL = "originalUrl";
    public static final String PROPERTY_QUERY        = "query";
    public static final String PROPERTY_PASSTHROUGH  = "passthrough";
    public static final String PROPERTY_URL          = "url";

    private UUID       id;
    private JsonObject attachments;
    private JsonObject body;
    private JsonObject headers;
    private String     method;
    private String     originalUrl;
    private JsonObject query;
    private JsonObject passthrough;
    private String     url;

    /**
     * Creates a message with headers, body, method, url, originalUrl, query and attachments.
     *
     * @param id          id of the message
     * @param attachments attachments of the message
     * @param body        body of the message
     * @param headers     headers of the message
     * @param method      method of the message
     * @param originalUrl original URL of the message
     * @param query       query of the message
     * @param passthrough passthrough of the message
     * @param url         URL of the message
     */
    private Message(final UUID        id,
                    final JsonObject attachments,
                    final JsonObject body,
                    final JsonObject headers,
                    final String     method,
                    final String     originalUrl,
                    final JsonObject query,
                    final JsonObject passthrough,
                    final String url) {
        if (id == null) {
            throw new IllegalArgumentException("Message id must not be null");
        }
        if (attachments == null) {
            throw new IllegalArgumentException("Message attachments must not be null");
        }
        if (body == null) {
            throw new IllegalArgumentException("Message body must not be null");
        }
        if (headers == null) {
            throw new IllegalArgumentException("Message headers must not be null");
        }
        if (passthrough == null) {
            throw new IllegalArgumentException("Message passthrough must not be null");
        }
        this.id = id;
        this.attachments = attachments;
        this.body = body;
        this.headers = headers;
        this.method = method;
        this.originalUrl = originalUrl;
        this.query = query;
        this.passthrough = passthrough;
        this.url = url;
    }

    /**
     * Returns message id.
     *
     * @return id
     */
    public UUID getId() {
        return id;
    }

    /**
     * Returns message attachments.
     *
     * @return attachments
     */
    public JsonObject getAttachments() {
        return attachments;
    }

    /**
     * Returns message body.
     *
     * @return body
     */
    public JsonObject getBody() {
        return body;
    }

    /**
     * Returns message headers.
     *
     * @return headers
     */
    public JsonObject getHeaders() {
        return headers;
    }

    /**
     * Returns message method.
     *
     * @return method
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns message originalUrl.
     *
     * @return originalUrl
     */
    public String getOriginalUrl() {
        return originalUrl;
    }

    /**
     * Returns message query object.
     *
     * @return query
     */
    public JsonObject getQuery() {
        return query;
    }

    /**
     * Returns message passthrough.
     *
     * @return passthrough
     */
    public JsonObject getPassthrough() {
        return passthrough;
    }

    /**
     * Returns message URL.
     *
     * @return url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns this message as {@link JsonObject}.
     *
     * @return message as JSON object
     */
    public JsonObject toJsonObject() {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add(PROPERTY_ID, id.toString())
                .add(PROPERTY_ATTACHMENTS, attachments)
                .add(PROPERTY_BODY, body)
                .add(PROPERTY_HEADERS, headers)
                .add(PROPERTY_PASSTHROUGH, passthrough);
        addJsonFieldToBuilderIfExist(builder, PROPERTY_METHOD, method);
        addJsonFieldToBuilderIfExist(builder, PROPERTY_ORIGINAL_URL, originalUrl);
        addJsonFieldToBuilderIfExist(builder, PROPERTY_QUERY, query);
        addJsonFieldToBuilderIfExist(builder, PROPERTY_URL, url);
        return builder.build();
    }

    private static JsonObjectBuilder addJsonFieldToBuilderIfExist(JsonObjectBuilder builder, String name, Object value) {
        if (value != null) {
            if (value instanceof JsonObject) {
                builder.add(name, (JsonObject) value);
            } else if (value instanceof String) {
                builder.add(name, (String) value);
            } else {
                throw new RuntimeException("Unknown type of the property to add to the Message");
            }
        }
        return builder;
    }

    @Override
    public String toString() {
        final JsonObject json = toJsonObject();
        final StringWriter writer = new StringWriter();
        final JsonWriter jsonWriter = Json.createWriter(writer);
        jsonWriter.writeObject(json);
        jsonWriter.close();
        return writer.toString();
    }

    /**
     * Used to build {@link Message} instances.
     */
    public static final class Builder {
        private UUID id;
        private JsonObject attachments;
        private JsonObject body;
        private JsonObject headers;
        private String method;
        private String originalUrl;
        private JsonObject query;
        private JsonObject passthrough;
        private String url;

        /**
         * Default constructor.
         */
        public Builder() {
            this.id = UUID.randomUUID();
            this.attachments = Json.createObjectBuilder().build();
            this.body = Json.createObjectBuilder().build();
            this.headers = Json.createObjectBuilder().build();
            this.passthrough = Json.createObjectBuilder().build();
        }

        /**
         * Sets message id.
         *
         * @param id id for the message
         * @return same builder instance
         */
        public Builder id(final UUID id) {
            this.id = id;
            return this;
        }

        /**
         * Adds attachments to build message with.
         *
         * @param attachments attachments for the message
         * @return same builder instance
         */
        public Builder attachments(final JsonObject attachments) {
            this.attachments = attachments;
            return this;
        }

        /**
         * Adds a body to build message with.
         *
         * @param body body for the message
         * @return same builder instance
         */
        public Builder body(final JsonObject body) {
            this.body = body;
            return this;
        }

        /**
         * Adds a headers to build message with.
         *
         * @param headers headers for the message
         * @return same builder instance
         */
        public Builder headers(final JsonObject headers) {
            this.headers = headers;
            return this;
        }

        /**
         * Adds method to build message with.
         *
         * @param method method for the message
         * @return same builder instance
         */
        public Builder method(final String method) {
            this.method = method;
            return this;
        }

        /**
         * Adds originalUrl to build message with.
         *
         * @param originalUrl originalUrl for the message
         * @return same builder instance
         */
        public Builder originalUrl(final String originalUrl) {
            this.originalUrl = originalUrl;
            return this;
        }

        /**
         * Adds query to build message with.
         *
         * @param query query for the message
         * @return same builder instance
         */
        public Builder query(final JsonObject query) {
            this.query = query;
            return this;
        }

        /**
         * Adds passthrough to build message with.
         *
         * @param passthrough passthrough for the message
         * @return same builder instance
         */
        public Builder passthrough(final JsonObject passthrough) {
            this.passthrough = passthrough;
            return this;
        }

        /**
         * Adds url to build message with.
         *
         * @param url url for the message
         * @return same builder instance
         */
        public Builder url(final String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds a {@link Message} instance and returns it.
         *
         * @return Message
         */
        public Message build() {
            return new Message(
                this.id,
                this.attachments,
                this.body,
                this.headers,
                this.method,
                this.originalUrl,
                this.query,
                this.passthrough,
                this.url);
        }
    }
}
