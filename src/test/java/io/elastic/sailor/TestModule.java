package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provider;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import io.elastic.sailor.component.SimpleSelectModelProvider;

public class TestModule extends AbstractModule {

    @Override
    protected void configure() {

        bindEnvVars();
    }

    void bindEnvVars() {
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "testCryptoPassword");
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, "iv=any16_symbols");

        bindRequiredStringEnvVar(Constants.ENV_VAR_COMPONENT_PATH,
                "src/test/java/io/elastic/sailor/component");

        bindRequiredStringEnvVar(Constants.ENV_VAR_AMQP_URI,
                "amqp://guest:guest@some-rabbit-server.com:5672");
        bindRequiredStringEnvVar(Constants.ENV_VAR_LISTEN_MESSAGES_ON,
                "5559edd38968ec0736000003:test_exec:step_1:messages");
        bindRequiredStringEnvVar(Constants.ENV_VAR_PUBLISH_MESSAGES_TO,
                "5527f0ea43238e5d5f000002_exchange");
        bindRequiredStringEnvVar(Constants.ENV_VAR_DATA_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.message");
        bindRequiredStringEnvVar(Constants.ENV_VAR_ERROR_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.error");
        bindRequiredStringEnvVar(Constants.ENV_VAR_REBOUND_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.rebound");
        bindRequiredStringEnvVar(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.snapshot");

        bindRequiredStringEnvVar(Constants.ENV_VAR_POST_RESULT_URL, "http://localhost:10000");
        bindRequiredStringEnvVar(Constants.ENV_VAR_ACTION_OR_TRIGGER, "helloworldaction");
        bindRequiredStringEnvVar(Constants.ENV_VAR_CFG, "{\"key\":0}");

        bindRequiredIntegerEnvVar(Constants.ENV_VAR_REBOUND_LIMIT, 5);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_REBOUND_INITIAL_EXPIRATION, 10000);

        bindRequiredStringEnvVar(Constants.ENV_VAR_STEP_ID, "step_1");
        bindRequiredStringEnvVar(Constants.ENV_VAR_TASK,
                "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");

        bindRequiredStringEnvVar(Constants.ENV_VAR_GET_MODEL_METHOD, SimpleSelectModelProvider.class.getName());
    }

    void bindRequiredStringEnvVar(final String name, final String value) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindRequiredIntegerEnvVar(final String name, final Integer value) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

}