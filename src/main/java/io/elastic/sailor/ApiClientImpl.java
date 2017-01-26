package io.elastic.sailor;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.JsonObject;

public class ApiClientImpl implements ApiClient {

    private static final Logger logger = LoggerFactory.getLogger(ApiClientImpl.class.getName());

    private final String apiUser;
    private final String apiKey;
    private final String apiBaseUri;

    @Inject
    public ApiClientImpl(@Named(Constants.ENV_VAR_API_URI) final String apiUri,
                         @Named(Constants.ENV_VAR_API_USERNAME) final String apiUser,
                         @Named(Constants.ENV_VAR_API_KEY) final String apiKey) {
        this.apiUser = apiUser;
        this.apiKey = apiKey;
        this.apiBaseUri = String.format("%s/v1", apiUri);
    }

    @Override
    public Step retrieveTaskStep(final String taskId, final String stepId) {
        final String uri = String.format("%s/tasks/%s/steps/%s", this.apiBaseUri, taskId, stepId);

        logger.info("Retrieving step data for user {} at: {}", this.apiUser, uri);

        final UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(this.apiUser, this.apiKey);

        final JsonObject step = HttpUtils.getJson(uri, credentials);

        return new Step(step);
    }

    @Override
    public JsonObject updateAccount(final String accountId, final JsonObject body) {
        final String uri = String.format("%s/accounts/%s", this.apiBaseUri, accountId);

        logger.info("Updating account for user {} at: {}", this.apiUser, uri);

        final UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(this.apiUser, this.apiKey);

        return HttpUtils.putJson(uri, body, credentials);

    }
}
