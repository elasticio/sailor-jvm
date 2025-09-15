package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ObjectStorage;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.JsonObject;

import java.io.ByteArrayInputStream;

public class ObjectStorageImpl implements ObjectStorage {

    private static final Logger logger = LoggerFactory.getLogger(ObjectStorageImpl.class);

    private CryptoServiceImpl cryptoService;
    private String objectStorageUri;
    private String objectStorageToken;
    private final CloseableHttpClient httpClient;

    public ObjectStorageImpl() {
        this.httpClient = HttpUtils.createHttpClient(5);
    }

    @Override
    public JsonObject getJsonObject(final String id) {

        if (this.objectStorageUri == null) {
            logger.info("Object storage service URI is not set");
            return null;
        }

        if (this.objectStorageToken == null) {
            logger.info("Object storage auth token is not set");
            return null;
        }

        this.logger.info("About to retrieve object by id={}", id);

        final String endpoint = String.format("%s/objects/%s", this.objectStorageUri, id);

        final byte[] bytes = HttpUtils.get(endpoint,
                this.httpClient,
                new HttpUtils.BearerAuthorizationHandler(this.objectStorageToken));
        return cryptoService.decryptMessageContent(bytes, MessageEncoding.UTF8);
    }

    @Override
    public JsonObject postJsonObject(final JsonObject object) {
        return post(object.toString());
    }

    @Override
    public JsonObject post(String object) {
        if (this.objectStorageUri == null) {
            logger.info("Object storage service URI is not set");
            return null;
        }

        if (this.objectStorageToken == null) {
            logger.info("Object storage auth token is not set");
            return null;
        }

        this.logger.info("About to post an object into the storage");

        final String endpoint = String.format("%s/objects/", this.objectStorageUri);

        final byte[] content = cryptoService.encrypt(object, MessageEncoding.UTF8);


        final JsonObject result = HttpUtils.post(endpoint,
                this.httpClient,
                new InputStreamEntity(new ByteArrayInputStream(content)),
                new HttpUtils.BearerAuthorizationHandler(this.objectStorageToken));

        return result;
    }

    @Inject
    public void setCryptoService(final CryptoServiceImpl cryptoService) {
        this.cryptoService = cryptoService;
    }

    @Inject(optional = true)
    public void setObjectStorageUri(final @Named(Constants.ENV_VAR_OBJECT_STORAGE_URI) String objectStorageUri) {
        this.objectStorageUri = objectStorageUri != null ? objectStorageUri.trim() : null;

        if (this.objectStorageUri != null && this.objectStorageUri.endsWith("/")) {
            this.objectStorageUri = this.objectStorageUri.substring(0, this.objectStorageUri.length() - 1);
        }
    }

    @Inject(optional = true)
    public void setObjectStorageToken(final @Named(Constants.ENV_VAR_OBJECT_STORAGE_TOKEN) String objectStorageToken) {
        this.objectStorageToken = objectStorageToken;
    }
}
