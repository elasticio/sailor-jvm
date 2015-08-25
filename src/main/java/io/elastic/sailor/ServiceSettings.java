package io.elastic.sailor;

public final class ServiceSettings {

    public static final String ENV_VAR_POST_RESULT_URL = "POST_RESULT_URL";
    public static final String ENV_VAR_CFG = "CFG";
    public static final String ENV_VAR_ACTION_OR_TRIGGER = "ACTION_OR_TRIGGER";
    public static final String ENV_VAR_GET_MODEL_METHOD = "GET_MODEL_METHOD";
    public static final String ENV_VAR_SLUG_URL = "SLUG_URL";
    public static final String ENV_VAR_MESSAGE_CRYPTO_PASSWORD = "MESSAGE_CRYPTO_PASSWORD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_IV = "MESSAGE_CRYPTO_IV";
    public static final String ENV_VAR_COMPONENT_PATH = "COMPONENT_PATH";
    public static final String ENV_VAR_AMQP_URI = "AMQP_URI";
    public static final String ENV_VAR_LISTEN_MESSAGES_ON = "LISTEN_MESSAGES_ON";

    public static String getEnvVarPostResultUrl() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_POST_RESULT_URL);
    }

    public static String getMessageCryptoPasswort() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD);
    }

    public static String getMessageCryptoIV() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV);
    }

    public static String getComponentPath() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_COMPONENT_PATH);
    }

    public static String getAmqpUri() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_AMQP_URI);
    }

    public static String getListenMessagesOn() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_LISTEN_MESSAGES_ON);
    }
}
