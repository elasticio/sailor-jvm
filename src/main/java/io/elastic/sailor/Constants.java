package io.elastic.sailor;

public final class Constants {

    public static final String NAME_TASK_JSON = "TaskJson";
    public static final String NAME_CFG_JSON = "ConfigurationJson";
    public static final String NAME_CALLBACK_DATA = "dataCallback";
    public static final String NAME_CALLBACK_REBOUND = "reboundCallback";
    public static final String NAME_CALLBACK_ERROR = "errorCallback";
    public static final String NAME_CALLBACK_SNAPSHOT = "snapshotCallback";

    public static final int DEFAULT_REBOUND_LIMIT = 20;
    public static final int DEFAULT_REBOUND_INITIAL_EXPIRATION = 15000;

    public static final String ENV_VAR_POST_RESULT_URL = "POST_RESULT_URL";
    public static final String ENV_VAR_CFG = "CFG";
    public static final String ENV_VAR_ACTION_OR_TRIGGER = "ACTION_OR_TRIGGER";
    public static final String ENV_VAR_FUNCTION = "FUNCTION";
    public static final String ENV_VAR_GET_MODEL_METHOD = "GET_MODEL_METHOD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_PASSWORD = "MESSAGE_CRYPTO_PASSWORD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_IV = "MESSAGE_CRYPTO_IV";
    public static final String ENV_VAR_AMQP_URI = "AMQP_URI";
    public static final String ENV_VAR_LISTEN_MESSAGES_ON = "LISTEN_MESSAGES_ON";
    public static final String ENV_VAR_PUBLISH_MESSAGES_TO = "PUBLISH_MESSAGES_TO";
    public static final String ENV_VAR_DATA_ROUTING_KEY = "DATA_ROUTING_KEY";
    public static final String ENV_VAR_ERROR_ROUTING_KEY = "ERROR_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_ROUTING_KEY = "REBOUND_ROUTING_KEY";
    public static final String ENV_VAR_SNAPSHOT_ROUTING_KEY = "SNAPSHOT_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_LIMIT = "REBOUND_LIMIT";
    public static final String ENV_VAR_REBOUND_INITIAL_EXPIRATION = "REBOUND_INITIAL_EXPIRATION";
    public static final String ENV_VAR_TASK = "TASK";
    public static final String ENV_VAR_STEP_ID = "STEP_ID";

}
