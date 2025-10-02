package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.api.JSON;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.*;
import java.util.ArrayList;
import java.util.List;

public class MessageResolverImpl implements MessageResolver {

    public static final int OBJECT_STORAGE_SIZE_THRESHOLD_DEFAULT = 1024 * 1024;
    private static final Logger logger = LoggerFactory.getLogger(MessageResolverImpl.class);

    private ComponentDescriptorResolver componentDescriptorResolver;
    private Step step;
    private ObjectStorage objectStorage;
    private CryptoServiceImpl cryptoService;
    private int objectStorageSizeThreshold = OBJECT_STORAGE_SIZE_THRESHOLD_DEFAULT;
    private MessageFormat messageFormat;

    @Override
    public Message materialize(final byte[] body, final AMQP.BasicProperties properties) {

        if (messageFormat == MessageFormat.ERROR) {
            return createErrorMessage(body, properties);
        }

        final MessageEncoding encoding = Utils.getMessageEncoding(properties);

        final JsonObject payload = cryptoService.decryptMessageContent(body, encoding);

        final String function = step.getFunction();

        final JsonObject moduleObject = componentDescriptorResolver.findModuleObject(function);

        final boolean autoResolveObjectReferences = moduleObject.getBoolean("autoResolveObjectReferences", true);

        if (!autoResolveObjectReferences) {
            logger.debug("Function is configured not to retrieve message body from object storage.");
            return Utils.createMessage(payload);
        }

        this.logger.debug("About to retrieve message body from storage");

        final JsonObjectBuilder resolved = resolveMessage(payload);

        if (resolved == null) {
            logger.debug("Message will be emitted as is");
            return Utils.createMessage(payload);
        }

        final JsonObject passthrough = payload.getJsonObject(Message.PROPERTY_PASSTHROUGH);

        final JsonObjectBuilder passthroughBuilder = Json.createObjectBuilder();

        if (passthrough != null) {
            this.logger.debug("About to retrieve passthrough from storage");

            for (String stepId : passthrough.keySet()) {
                final JsonObjectBuilder resolvedStep = resolveMessage(passthrough.getJsonObject(stepId));

                if (resolvedStep != null) {
                    passthroughBuilder.add(stepId, resolvedStep);
                }
            }
        }

        resolved.add(Message.PROPERTY_PASSTHROUGH, passthroughBuilder);

        return Utils.createMessage(resolved.build());
    }

    private Message createErrorMessage(final byte[] body, final AMQP.BasicProperties properties) {
        final JsonObject errorBody = JSON.parse(body);
        logger.debug("Error message:{}", new String(body));

        final JsonObjectBuilder headers = Json.createObjectBuilder();
        final JsonObjectBuilder builder = Json.createObjectBuilder();

        decryptPropertyAndAddToBuilder(errorBody, ErrorPublisherImpl.ERROR_PROPERTY, builder);
        decryptPropertyAndAddToBuilder(errorBody, ErrorPublisherImpl.ERROR_INPUT_PROPERTY, builder);

        properties.getHeaders().entrySet()
                .stream()
                .forEach(s -> headers.add(s.getKey(), s.getValue().toString()));
        ;

        return new Message.Builder()
                .body(builder.build())
                .headers(headers.build())
                .build();
    }

    private void decryptPropertyAndAddToBuilder(final JsonObject object,
                                                final String propertyName,
                                                final JsonObjectBuilder builder) {
        final JsonString value = object.getJsonString(propertyName);

        if (value == null) {
            return;
        }

        final JsonObject decrypted = cryptoService.decryptMessageContent(
                value.getString().getBytes(), MessageEncoding.BASE64);

        builder.add(propertyName, decrypted);
    }

    @Override
    public JsonObject externalize(final JsonObject message) {
        logger.debug("Externalizing message body");
        final MessageHolder messageHolder = new MessageHolder(message);

        final List<MessageHolder> passthroughHolders = new ArrayList<>();
        final JsonObject passthrough = message.getJsonObject(Message.PROPERTY_PASSTHROUGH);

        if (passthrough != null) {
            for (String stepId : passthrough.keySet()) {
                logger.debug("Externalizing passthrough step={}", stepId);
                final JsonObject msg = passthrough.getJsonObject(stepId);
                passthroughHolders.add(new MessageHolder(stepId, msg));
            }
        }

        final Integer passthroughSize = passthroughHolders.stream()
                .map(e -> e.bodyStr.length())
                .reduce(0, (subtotal, element) -> subtotal + element);

        int totalSize = messageHolder.bodyStr.getBytes().length + passthroughSize;

        logger.debug("Message total size (body+passthrough): {} bytes", totalSize);

        if (totalSize <= this.objectStorageSizeThreshold) {
            logger.debug("Message size is below the threshold of {} bytes. No externalization required."
                    , this.objectStorageSizeThreshold);
            return message;
        }

        final JsonObjectBuilder result = externalizeObject(messageHolder);
        final JsonObjectBuilder passthroughBuilder = Json.createObjectBuilder();

        for (MessageHolder next : passthroughHolders) {
            logger.debug("Externalizing passthrough step={}", next.stepId);
            final JsonObjectBuilder externalizedStep = externalizeObject(next);
            passthroughBuilder.add(next.stepId, externalizedStep);
        }

        result.add(Message.PROPERTY_PASSTHROUGH, passthroughBuilder);

        return result.build();
    }

    private JsonObjectBuilder externalizeObject(final MessageHolder holder) {

        final JsonObjectBuilder result = Utils.copy(holder.message);

        final JsonObject storedObject = objectStorage.post(holder.bodyStr);

        final JsonValue objectId = storedObject.get("objectId");

        logger.debug("Stored object with id={}", objectId);

        final JsonObject headers = holder.message.getJsonObject(Message.PROPERTY_HEADERS);

        JsonObjectBuilder headersBuilder;

        if (headers == null) {
            headersBuilder = Json.createObjectBuilder();
        } else {
            headersBuilder = Utils.copy(headers);
        }

        headersBuilder.add(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID, objectId);

        result.add(Message.PROPERTY_HEADERS, headersBuilder.build());
        result.add(Message.PROPERTY_BODY, Json.createObjectBuilder().build());

        return result;
    }

    private JsonObjectBuilder resolveMessage(final JsonObject message) {

        final JsonObject headers = getNonNullJsonObject(message, Message.PROPERTY_HEADERS);

        final JsonString objectId = headers.getJsonString(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID);

        if (objectId == null) {
            logger.debug("No id to retrieve the object from storage found");
            return null;
        }

        final JsonObject object = this.objectStorage.getJsonObject(objectId.getString());

        final JsonObject cleanedHeaders = Utils.omit(headers, Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID);

        final JsonObjectBuilder builder = Json.createObjectBuilder()
                .add(Message.PROPERTY_BODY, object)
                .add(Message.PROPERTY_HEADERS, cleanedHeaders)
                .add(Message.PROPERTY_ATTACHMENTS, getNonNullJsonObject(message, Message.PROPERTY_ATTACHMENTS));

        final JsonString messageId = message.getJsonString(Message.PROPERTY_ID);

        if (messageId != null) {
            builder.add(Message.PROPERTY_ID, messageId.getString());
        }

        return builder;
    }


    @Inject
    public void setCryptoService(final CryptoServiceImpl cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Inject
    public void setComponentDescriptorResolver(final ComponentDescriptorResolver componentDescriptorResolver) {
        this.componentDescriptorResolver = componentDescriptorResolver;
    }

    @Inject
    public void setStep(@Named(Constants.NAME_STEP_JSON) final Step step) {
        this.step = step;
    }

    @Inject
    public void setObjectStorage(final ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }


    @Inject
    public void setObjectStorageSizeThreshold(final @Named(Constants.ENV_VAR_OBJECT_STORAGE_SIZE_THRESHOLD)
                                                      int objectStorageSizeThreshold) {
        this.objectStorageSizeThreshold = objectStorageSizeThreshold;
    }

    @Inject
    public void setMessageFormat(final @Named(Constants.ENV_VAR_INPUT_FORMAT) MessageFormat messageFormat) {
        this.messageFormat = messageFormat;
    }


    private JsonObject getNonNullJsonObject(final JsonObject object, final String property) {
        final JsonObject value = object.getJsonObject(property);

        if (value == null) {
            return Json.createObjectBuilder().build();
        }

        return value;
    }

    private class MessageHolder {
        private String stepId;
        private JsonObject message;
        private String bodyStr;

        public MessageHolder(final JsonObject message) {
            this("", message);
        }

        public MessageHolder(final String stepId, final JsonObject message) {
            this.stepId = stepId;
            this.message = message;
            this.bodyStr = message.getJsonObject(Message.PROPERTY_BODY).toString();
        }

    }
}
