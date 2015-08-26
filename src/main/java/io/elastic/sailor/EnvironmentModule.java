package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Names;

public class EnvironmentModule extends AbstractModule {

    @Override
    protected void configure() {
        bindEnvVars();
    }

    void bindEnvVars() {
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_COMPONENT_PATH);
    }

    void bindRequiredStringEnvVar(final String name) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(Utils.getEnvVar(name));
    }
}
