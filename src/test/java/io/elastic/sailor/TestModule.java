package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class TestModule extends AbstractModule {

    @Override
    protected void configure() {

        bindEnvVars();
    }

    void bindEnvVars() {
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "testCryptoPassword");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "iv=any16_symbols");

        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_COMPONENT_PATH,
                "src/test/java/io/elastic/sailor/component");

        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_AMQP_URI,
                "amqp://guest:guest@some-rabbit-server.com:5672");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_LISTEN_MESSAGES_ON,
                "5559edd38968ec0736000003:test_exec:step_1:messages");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_PUBLISH_MESSAGES_TO,
                "5527f0ea43238e5d5f000002_exchange");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_DATA_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.message");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_ERROR_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.error");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_REBOUND_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.rebound");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_SNAPSHOT_ROUTING_KEY,
                "5559edd38968ec0736000003.test_exec.step_1.snapshot");

        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_POST_RESULT_URL, "http://localhost:10000");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_ACTION_OR_TRIGGER, "test");
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_CFG, "{\"key\":0}");

        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_REBOUND_LIMIT, 5);
        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_REBOUND_INITIAL_EXPIRATION, 10000);

        bindRequiredStringEnvVar(ServiceSettings.ENV_VAR_TASK,
                "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");
    }

    void bindRequiredStringEnvVar(final String name, final String value) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindRequiredStringEnvVar(final String name, final Integer value) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }
}
