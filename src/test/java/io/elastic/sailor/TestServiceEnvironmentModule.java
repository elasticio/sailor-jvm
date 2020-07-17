package io.elastic.sailor;

import io.elastic.sailor.component.SimpleSelectModelProvider;

public class TestServiceEnvironmentModule extends AbstractSailorTestModule {

    @Override
    protected void configure() {

        bindEnvVars();
    }

    void bindEnvVars() {

        bindRequiredStringEnvVar(Constants.ENV_VAR_POST_RESULT_URL, "http://admin:secret@localhost:10000");
        bindRequiredStringEnvVar(Constants.ENV_VAR_ACTION_OR_TRIGGER, "helloworldaction");
        bindRequiredStringEnvVar(Constants.ENV_VAR_CFG, "{\"key\":0}");
        bindRequiredStringEnvVar(Constants.ENV_VAR_GET_MODEL_METHOD, SimpleSelectModelProvider.class.getName());
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS, "0");
    }
}
