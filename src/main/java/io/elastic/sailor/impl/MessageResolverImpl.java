package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.nio.charset.Charset;

public class MessageResolverImpl implements MessageResolver {

    private static final Logger logger = LoggerFactory.getLogger(MessageResolverImpl.class);

    private ComponentDescriptorResolver componentDescriptorResolver;
    private Step step;
    private CryptoServiceImpl cryptoService;
    private String objectStorageUri;
    private String objectStorageToken;

    @Override
    public Message resolve(byte[] body) {

        final String bodyString = new String(body, Charset.forName("UTF-8"));
        final JsonObject payload = cryptoService.decryptMessageContent(bodyString);

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
            passthroughBuilder.add(stepId, resolvedStep);
        }

        resolved.add(Message.PROPERTY_PASSTHROUGH, passthroughBuilder);

        return Utils.createMessage(resolved.build());
    }

    private JsonObjectBuilder resolveMessage(final JsonObject message) {

        final JsonObject headers = getNonNullJsonObject(message, Message.PROPERTY_HEADERS);

        if (this.objectStorageUri == null) {
            logger.info("Object storage service URI is not set");
            return null;
        }

        if (this.objectStorageToken == null) {
            logger.info("Object storage auth token is not set");
            return null;
        }

        final JsonString objectId = headers.getJsonString(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID);

        if (objectId == null) {
            logger.info("No id to retrieve the object from storage found");
            return null;
        }

        final String objectIdStr = objectId.getString();

        this.logger.info("About to retrieve object by id={}", objectIdStr);

        final String endpoint = String.format("%s/objects/%s", this.objectStorageUri, objectIdStr);

        final String content = HttpUtils.get(endpoint,
                new HttpUtils.BearerAuthorizationHandler(this.objectStorageToken),
                5);

        final JsonObject object = cryptoService.decryptMessageContent(content);

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

    @Inject(optional = true)
    public void setObjectStorageUri(final @Named(Constants.ENV_VAR_OBJECT_STORAGE_URI) String objectStorageUri) {
        this.objectStorageUri = objectStorageUri;
    }

    @Inject(optional = true)
    public void setObjectStorageToken(final @Named(Constants.ENV_VAR_OBJECT_STORAGE_TOKEN) String objectStorageToken) {
        this.objectStorageToken = objectStorageToken;
    }

    @Inject
    public void setComponentDescriptorResolver(final ComponentDescriptorResolver componentDescriptorResolver) {
        this.componentDescriptorResolver = componentDescriptorResolver;
    }

    @Inject
    public void setStep(@Named(Constants.NAME_STEP_JSON) final Step step) {
        this.step = step;
    }


    private JsonObject getNonNullJsonObject(final JsonObject object, final String property) {
        final JsonObject value = object.getJsonObject(property);

        if (value == null) {
            return Json.createObjectBuilder().build();
        }

        return value;
    }
}
