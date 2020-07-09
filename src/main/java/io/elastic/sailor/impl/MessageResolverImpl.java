package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class MessageResolverImpl implements MessageResolver {

    public static final int OBJECT_STORAGE_SIZE_THRESHOLD_DEFAULT = 1024*1024;
    private static final Logger logger = LoggerFactory.getLogger(MessageResolverImpl.class);

    private ComponentDescriptorResolver componentDescriptorResolver;
    private Step step;
    private ObjectStorage objectStorage;
    private CryptoServiceImpl cryptoService;
    private int objectStorageSizeThreshold = OBJECT_STORAGE_SIZE_THRESHOLD_DEFAULT;

    @Override
    public Message materialize(byte[] body) {

        final JsonObject payload = cryptoService.decryptMessageContent(body, MessageEncoding.BASE64);

        final String function = step.getFunction();

        final JsonObject moduleObject = componentDescriptorResolver.findModuleObject(function);

        final boolean autoResolveObjectReferences = moduleObject.getBoolean("autoResolveObjectReferences", true);

        if (!autoResolveObjectReferences) {
            logger.info("Function is configured not to retrieve message body from object storage.");
            return Utils.createMessage(payload);
        }

        this.logger.info("About to retrieve message body from storage");

        final JsonObjectBuilder resolved = resolveMessage(payload);

        if (resolved == null) {
            logger.info("Message will be emitted as is");
            return Utils.createMessage(payload);
        }

        final JsonObject passthrough = payload.getJsonObject(Message.PROPERTY_PASSTHROUGH);

        final JsonObjectBuilder passthroughBuilder = Json.createObjectBuilder();

        this.logger.info("About to retrieve passthrough from storage");

        for (String stepId : passthrough.keySet()) {
            final JsonObjectBuilder resolvedStep = resolveMessage(passthrough.getJsonObject(stepId));

            if (resolvedStep != null) {
                passthroughBuilder.add(stepId, resolvedStep);
            }
        }

        resolved.add(Message.PROPERTY_PASSTHROUGH, passthroughBuilder);

        return Utils.createMessage(resolved.build());
    }

    @Override
    public JsonObject externalize(final JsonObject message) {
        logger.info("Externalizing message body");
        final MessageHolder messageHolder = new MessageHolder(message);

        final List<MessageHolder> passthroughHolders = new ArrayList<>();
        final JsonObject passthrough = message.getJsonObject(Message.PROPERTY_PASSTHROUGH);

        for (String stepId : passthrough.keySet()) {
            logger.info("Externalizing passthrough step={}", stepId);
            final JsonObject msg = passthrough.getJsonObject(stepId);
            passthroughHolders.add(new MessageHolder(stepId, msg));
        }

        final Integer passthroughSize = passthroughHolders.stream()
                .map(e -> e.bodyStr.length())
                .reduce(0, (subtotal, element) -> subtotal + element);

        int totalSize = messageHolder.bodyStr.getBytes().length + passthroughSize;

        logger.info("Message total size (body+passthrough): {} bytes", totalSize);

        if (totalSize <= this.objectStorageSizeThreshold) {
            logger.info("Message size is below the threshold of {} bytes. No externalization required."
                    , this.objectStorageSizeThreshold);
            return message;
        }

        final JsonObjectBuilder result = externalizeObject(messageHolder);
        final JsonObjectBuilder passthroughBuilder = Json.createObjectBuilder();

        for (MessageHolder next : passthroughHolders) {
            logger.info("Externalizing passthrough step={}", next.stepId);
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

        logger.info("Stored object with id={}", objectId);

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
            logger.info("No id to retrieve the object from storage found");
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
