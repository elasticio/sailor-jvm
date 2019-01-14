package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.elastic.sailor.component.SimpleSelectModelProvider;

public class TestServiceEnvironmentModule extends AbstractModule {

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

    void bindRequiredStringEnvVar(final String name, final String value) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }
}
