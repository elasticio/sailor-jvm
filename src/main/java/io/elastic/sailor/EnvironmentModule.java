package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class EnvironmentModule extends AbstractModule {

    @Override
    protected void configure() {
        bindEnvVars();
    }

    void bindEnvVars() {
        // required env vars
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_COMPONENT_PATH);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_TASK);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_STEP_ID);


        // optional env vars
        bindOptionalIntegerEnvVar(
                ServiceSettings.ENV_VAR_REBOUND_LIMIT,
                ServiceSettings.DEFAULT_REBOUND_LIMIT);
        bindOptionalIntegerEnvVar(
                ServiceSettings.ENV_VAR_REBOUND_INITIAL_EXPIRATION,
                ServiceSettings.DEFAULT_REBOUND_INITIAL_EXPIRATION);
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

    private static int getOptionalIntegerValue(final String key, int defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Integer.parseInt(value);
        }

        return defaultValue;
    }
}
