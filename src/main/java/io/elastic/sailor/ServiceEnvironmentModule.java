package io.elastic.sailor;

public class ServiceEnvironmentModule extends AbstractSailorModule {

    @Override
    protected void configure() {
        bindEnvVars();
    }

    void bindEnvVars() {
        // required env vars
        bindRequiredStringEnvVar(Constants.ENV_VAR_CFG);
        bindRequiredStringEnvVar(Constants.ENV_VAR_POST_RESULT_URL);


        // optional env vars

        bindOptionalStringEnvVar(Constants.ENV_VAR_ACTION_OR_TRIGGER);
        bindOptionalStringEnvVar(Constants.ENV_VAR_GET_MODEL_METHOD);
        bindOptionalIntegerEnvVar(
                Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS,
                Constants.DEFAULT_API_REQUEST_RETRY_ATTEMPTS);
    }
}
