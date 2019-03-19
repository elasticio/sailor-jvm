package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class ServiceEnvironmentModule extends AbstractModule {

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

    void bindRequiredStringEnvVar(final String name) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(Utils.getEnvVar(name));
    }


    void bindOptionalStringEnvVar(final String name) {

        bind(String.class)
                .annotatedWith(Names.named(name))
                .toProvider(new Provider<String>() {

                    @Override
                    public String get() {
                        return Utils.getOptionalEnvVar(name);
                    }
                });
    }

    private static int getOptionalIntegerValue(final String key, int defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Integer.parseInt(value);
        }

        return defaultValue;
    }

    void bindOptionalIntegerEnvVar(final String name, int defaultValue) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalIntegerValue(name, defaultValue));
    }
}
