package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class SailorEnvironmentModule extends AbstractModule {

    @Override
    protected void configure() {
        bindEnvVars();
    }

    void bindEnvVars() {
        // required env vars
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_URI);
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_USERNAME);
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD);
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_IV);
        bindRequiredStringEnvVar(Constants.ENV_VAR_FLOW_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_STEP_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_FUNCTION);
        bindRequiredStringEnvVar(Constants.ENV_VAR_EXEC_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_USER_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_COMP_ID);

        bindRequiredStringEnvVar(Constants.ENV_VAR_AMQP_URI);
        bindRequiredStringEnvVar(Constants.ENV_VAR_LISTEN_MESSAGES_ON);
        bindRequiredStringEnvVar(Constants.ENV_VAR_PUBLISH_MESSAGES_TO);
        bindRequiredStringEnvVar(Constants.ENV_VAR_DATA_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_ERROR_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_REBOUND_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY);


        // optional env vars
        bindOptionalIntegerEnvVar(
                Constants.ENV_VAR_REBOUND_LIMIT,
                Constants.DEFAULT_REBOUND_LIMIT);
        bindOptionalIntegerEnvVar(
                Constants.ENV_VAR_REBOUND_INITIAL_EXPIRATION,
                Constants.DEFAULT_REBOUND_INITIAL_EXPIRATION);
        bindOptionalIntegerEnvVar(
                Constants.ENV_VAR_RABBITMQ_PREFETCH_SAILOR,
                Constants.DEFAULT_RABBITMQ_PREFETCH_SAILOR);

        bindOptionalYesNoEnvVar(Constants.ENV_VAR_STARTUP_REQUIRED);
        bindOptionalYesNoEnvVar(Constants.ENV_VAR_HOOK_SHUTDOWN);


    }

    void bindRequiredStringEnvVar(final String name) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(Utils.getEnvVar(name));
    }

    void bindOptionalIntegerEnvVar(final String name, int defaultValue) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalIntegerValue(name, defaultValue));
    }

    void bindOptionalYesNoEnvVar(final String name) {
        bind(Boolean.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalYesNoValue(name));
    }

    private static int getOptionalIntegerValue(final String key, int defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Integer.parseInt(value);
        }

        return defaultValue;
    }

    private static boolean getOptionalYesNoValue(final String key) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return true;
        }

        return false;
    }
}
