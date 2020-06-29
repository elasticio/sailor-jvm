package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.elastic.sailor.impl.MessageResolverImpl;

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
        bindRequiredStringEnvVar(Constants.ENV_VAR_FLOW_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_STEP_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_FUNCTION);
        bindRequiredStringEnvVar(Constants.ENV_VAR_EXEC_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_USER_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_COMP_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_CONTAINER_ID);
        bindRequiredStringEnvVar(Constants.ENV_VAR_WORKSPACE_ID);

        bindOptionalStringEnvVar(Constants.ENV_VAR_COMP_NAME);
        bindOptionalStringEnvVar(Constants.ENV_VAR_CONTRACT_ID);
        bindOptionalStringEnvVar(Constants.ENV_VAR_EXEC_TYPE);
        bindOptionalStringEnvVar(Constants.ENV_VAR_EXECUTION_RESULT_ID);
        bindOptionalStringEnvVar(Constants.ENV_VAR_FLOW_VERSION);
        bindOptionalStringEnvVar(Constants.ENV_VAR_TASK_USER_EMAIL);
        bindOptionalStringEnvVar(Constants.ENV_VAR_TENANT_ID);
        bindOptionalStringEnvVar(Constants.ENV_VAR_OBJECT_STORAGE_URI);
        bindOptionalStringEnvVar(Constants.ENV_VAR_OBJECT_STORAGE_TOKEN);


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
        bindOptionalIntegerEnvVar(
                Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS,
                Constants.DEFAULT_API_REQUEST_RETRY_ATTEMPTS);

        bindOptionalYesNoEnvVar(Constants.ENV_VAR_STARTUP_REQUIRED);
        bindOptionalYesNoEnvVar(Constants.ENV_VAR_NO_SELF_PASSTRHOUGH);
        bindOptionalYesNoEnvVar(Constants.ENV_VAR_HOOK_SHUTDOWN);
        bindOptionalYesNoEnvVar(Constants.ENV_VAR_EMIT_LIGHTWEIGHT_MESSAGE);

        bindOptionalIntegerEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_ATTEMPTS, Integer.MAX_VALUE);

        // 100 ms
        bindOptionalLongEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_DELAY, 100L);

        // 5 mins
        bindOptionalLongEnvVar(Constants.ENV_VAR_AMQP_PUBLISH_MAX_RETRY_DELAY, 5 * 60 * 1000L);

        // 1MB
        getOptionalIntegerValue(Constants.ENV_VAR_OBJECT_STORAGE_SIZE_THRESHOLD,
                MessageResolverImpl.OBJECT_STORAGE_SIZE_THRESHOLD_DEFAULT);


    }

    void bindRequiredStringEnvVar(final String name) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(Utils.getEnvVar(name));
    }

    void bindOptionalStringEnvVar(final String name) {

        final String value = Utils.getOptionalEnvVar(name);

        if (value == null) {
            return;
        }

        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindOptionalIntegerEnvVar(final String name, int defaultValue) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalIntegerValue(name, defaultValue));
    }

    void bindOptionalLongEnvVar(final String name, long defaultValue) {
        bind(Long.class)
                .annotatedWith(Names.named(name))
                .toInstance(getOptionalLongValue(name, defaultValue));
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

    private static long getOptionalLongValue(final String key, long defaultValue) {
        final String value = Utils.getOptionalEnvVar(key);

        if (value != null) {
            return Long.parseLong(value);
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
