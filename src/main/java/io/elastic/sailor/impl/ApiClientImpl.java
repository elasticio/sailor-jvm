package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.elastic.sailor.ApiClient;
import io.elastic.sailor.Constants;
import io.elastic.sailor.Step;
import io.elastic.sailor.impl.HttpUtils.BasicAuthorizationHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;

@Singleton
public class ApiClientImpl implements ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClientImpl.class.getName());

    private final BasicAuthorizationHandler authorizationHandler;
    private final String apiBaseUri;
    private final CloseableHttpClient httpClient;
    private boolean putIncomingMessageIntoPassThrough;

    @Inject
    public ApiClientImpl(@Named(Constants.ENV_VAR_API_URI) final String apiUri,
                         @Named(Constants.ENV_VAR_API_USERNAME) final String apiUser,
                         @Named(Constants.ENV_VAR_API_KEY) final String apiKey,
                         @Named(Constants.ENV_VAR_NO_SELF_PASSTRHOUGH) boolean putIncomingMessageIntoPassThrough,
                         final CloseableHttpClient httpClient) {

        this.authorizationHandler = new BasicAuthorizationHandler(apiUser, apiKey);
        this.apiBaseUri = String.format("%s", apiUri);
        this.httpClient = httpClient;
        this.putIncomingMessageIntoPassThrough = putIncomingMessageIntoPassThrough;
    }

    @Override
    public Step retrieveFlowStep(final String taskId, final String stepId) {
        final String path = String.format("/v1/tasks/%s/steps/%s", taskId, stepId);
        final String uri = this.apiBaseUri + path;

        logger.info("Retrieving step data at: {}", path);

        final JsonObject step = HttpUtils.getJson(uri, this.httpClient, authorizationHandler);

        return new Step(step, uri, authorizationHandler, putIncomingMessageIntoPassThrough);
    }

    @Override
    public JsonObject updateAccount(final String accountId, final JsonObject body) {
        final String path = String.format("/v1/accounts/%s", accountId);
        final String uri = this.apiBaseUri + path;

        logger.info("Updating account for user {} at: {}", this.authorizationHandler.getUsername(), path);

        return HttpUtils.putJson(uri, this.httpClient, body, authorizationHandler);

    }

    @Override
    public void storeStartupState(final String flowId, final JsonObject body) {
        final String uri = getStartupStateUrl(flowId);

        HttpUtils.postJson(uri, this.httpClient, body, authorizationHandler);
    }

    @Override
    public JsonObject retrieveStartupState(final String flowId) {
        final String uri = getStartupStateUrl(flowId);

        final JsonObject state = HttpUtils.getJson(uri, this.httpClient, authorizationHandler);

        if (state == null) {
            return Json.createObjectBuilder().build();
        }

        return state;
    }

    @Override
    public void deleteStartupState(final String flowId) {
        final String uri = getStartupStateUrl(flowId);

        HttpUtils.delete(uri, this.httpClient, authorizationHandler);
    }

    private String getStartupStateUrl(final String flowId) {
        return String.format("%s/sailor-support/hooks/task/%s/startup/data", this.apiBaseUri, flowId);
    }
}
