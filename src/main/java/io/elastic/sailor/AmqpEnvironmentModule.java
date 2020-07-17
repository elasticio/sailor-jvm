package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.elastic.sailor.impl.MessageEncoding;
import io.elastic.sailor.impl.MessageFormat;
import io.elastic.sailor.impl.MessageResolverImpl;

import static io.elastic.sailor.SailorEnvironmentModule.getOptionalIntegerValue;

public class AmqpEnvironmentModule extends AbstractModule {
    @Override
    protected void configure() {

        bindRequiredStringEnvVar(Constants.ENV_VAR_AMQP_URI);
        bindRequiredStringEnvVar(Constants.ENV_VAR_LISTEN_MESSAGES_ON);
        bindRequiredStringEnvVar(Constants.ENV_VAR_PUBLISH_MESSAGES_TO);
        bindRequiredStringEnvVar(Constants.ENV_VAR_DATA_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_ERROR_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_REBOUND_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY);
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD);
        bindRequiredStringEnvVar(Constants.ENV_VAR_MESSAGE_CRYPTO_IV);

        // 1MB
        bindOptionalIntegerEnvVar(Constants.ENV_VAR_OBJECT_STORAGE_SIZE_THRESHOLD,
                MessageResolverImpl.OBJECT_STORAGE_SIZE_THRESHOLD_DEFAULT);

        bindProtocolVersion();

        bindMessageFormat();

        bindNoErrorReplies();
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


    private  void bindProtocolVersion() {

        final int protocolVersion = getOptionalIntegerValue(Constants.ENV_VAR_PROTOCOL_VERSION,
                MessageEncoding.BASE64.protocolVersion);

        bind(MessageEncoding.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_PROTOCOL_VERSION))
                .toInstance(MessageEncoding.fromProtocolVersion(protocolVersion));
    }

    private void bindMessageFormat() {
        MessageFormat format = MessageFormat.DEFAULT;

        final String value = Utils.getOptionalEnvVar(Constants.ENV_VAR_INPUT_FORMAT);

        if (value != null) {
            format = MessageFormat.valueOf(value.toLowerCase());
        }

        bind(MessageFormat.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_INPUT_FORMAT))
                .toInstance(format);
    }

    void bindNoErrorReplies() {
        final boolean value = SailorEnvironmentModule.getOptionalBooleanValue(
                Constants.ENV_VAR_NO_ERROR_REPLIES, false);

        bind(Boolean.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_NO_ERROR_REPLIES))
                .toInstance(value);
    }
}
