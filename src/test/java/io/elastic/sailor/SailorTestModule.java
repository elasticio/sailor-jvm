package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class SailorTestModule extends AbstractModule {

    @Override
    protected void configure() {

        bindEnvVars();
    }

    void bindEnvVars() {

        bindRequiredStringEnvVar(Constants.ENV_VAR_API_URI, "http://localhost:11111");
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_USERNAME, "admin");
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_KEY, "secret");
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "testCryptoPassword");
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, "iv=any16_symbols");

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

        bindRequiredStringEnvVar(Constants.ENV_VAR_CFG, "{\"key\":0}");

        bindRequiredIntegerEnvVar(Constants.ENV_VAR_RABBITMQ_PREFETCH_SAILOR, 1);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_REBOUND_LIMIT, 5);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_REBOUND_INITIAL_EXPIRATION, 10000);

        bindRequiredStringEnvVar(Constants.ENV_VAR_STEP_ID, "step_1");
        bindRequiredStringEnvVar(Constants.ENV_VAR_TASK_ID, "5559edd38968ec0736000003");

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
