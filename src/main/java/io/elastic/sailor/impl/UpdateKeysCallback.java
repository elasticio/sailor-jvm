package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.EventEmitter;
import io.elastic.sailor.ApiClient;
import io.elastic.sailor.Constants;
import io.elastic.sailor.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;

public class UpdateKeysCallback implements EventEmitter.Callback {

    private static final Logger logger = LoggerFactory.getLogger(UpdateKeysCallback.class);

    private final Step step;
    private final ApiClient apiClient;

    @Inject
    public UpdateKeysCallback(@Named(Constants.NAME_STEP_JSON) final Step step,
                              final ApiClient apiClient) {
        this.step = step;
        this.apiClient = apiClient;
    }

    @Override
    public void receive(final Object object) {
        final String stepId = step.getId();

        logger.info("Update to update keys for step {}", stepId);

        final JsonObject keys = (JsonObject) object;

        final JsonObject body = Json.createObjectBuilder()
                .add("keys", keys)
                .build();

        final JsonObject config = step.getCfg();
        final JsonString accountId = config.getJsonString("_account");

        if (accountId == null) {
            throw new IllegalStateException(
                    "Component emitted 'updateKeys' event but no account is configured for step " + stepId);
        }

        apiClient.updateAccount(accountId.getString(), body);

        logger.info("Updated account");

        logger.info("Successfully updated keys for step {}", stepId);
    }
}
