package io.elastic.sailor;

import com.google.inject.name.Names;
import io.elastic.sailor.impl.MessageEncoding;
import io.elastic.sailor.impl.MessageFormat;

public class SailorTestModule extends AbstractSailorTestModule {

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
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_CONSUMER_THREAD_POOL_SIZE_SAILOR, 1);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_REBOUND_LIMIT, 5);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_REBOUND_INITIAL_EXPIRATION, 10000);

        bindRequiredStringEnvVar(Constants.ENV_VAR_CONTAINER_ID, "container_123");
        bindRequiredStringEnvVar(Constants.ENV_VAR_STEP_ID, "step_1");
        bindRequiredStringEnvVar(Constants.ENV_VAR_FLOW_ID, "5559edd38968ec0736000003");
        bindRequiredStringEnvVar(Constants.ENV_VAR_EXEC_ID, "some-exec-id");
        bindRequiredStringEnvVar(Constants.ENV_VAR_USER_ID, "5559edd38968ec0736000002");
        bindRequiredStringEnvVar(Constants.ENV_VAR_COMP_ID, "5559edd38968ec0736000456");
        bindRequiredStringEnvVar(Constants.ENV_VAR_WORKSPACE_ID, "workspace_123");
        bindRequiredStringEnvVar(Constants.ENV_VAR_FUNCTION, "myFunction");
        bind(Boolean.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_STARTUP_REQUIRED))
                .toInstance(false);
        bind(Boolean.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_NO_SELF_PASSTRHOUGH))
                .toInstance(false);

        bind(Boolean.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_HOOK_SHUTDOWN))
                .toInstance(false);

        bindRequiredIntegerEnvVar(Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS, 5);

        bindRequiredIntegerEnvVar(Constants.ENV_VAR_OBJECT_STORAGE_SIZE_THRESHOLD, Integer.MAX_VALUE);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_ATTEMPTS, Integer.MAX_VALUE);
        bindRequiredLongEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_DELAY, 100L);
        bindRequiredLongEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_MAX_RETRY_DELAY, 5 * 60 * 1000L);

        bindRequiredBooleanEnvVar(Constants.ENV_VAR_EMIT_LIGHTWEIGHT_MESSAGE, false);

        bindRequiredBooleanEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_CONFIRM_ENABLED, true);
        bindRequiredBooleanEnvVar(Constants.ENV_VAR_AMQP_AMQP_PERSISTENT_MESSAGES, true);

        bind(MessageEncoding.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_PROTOCOL_VERSION))
                .toInstance(MessageEncoding.BASE64);

        bind(MessageFormat.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_INPUT_FORMAT))
                .toInstance(MessageFormat.DEFAULT);

        bind(Boolean.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_NO_ERROR_REPLIES))
                .toInstance(true);

    }

}
