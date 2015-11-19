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
    public static final int DEFAULT_RABBITMQ_PREFETCH_SAILOR = 1;

    public static final String ENV_VAR_POST_RESULT_URL = "ELASTICIO_POST_RESULT_URL";
    public static final String ENV_VAR_CFG = "ELASTICIO_CFG";
    public static final String ENV_VAR_ACTION_OR_TRIGGER = "ELASTICIO_ACTION_OR_TRIGGER";
    public static final String ENV_VAR_FUNCTION = "ELASTICIO_FUNCTION";
    public static final String ENV_VAR_GET_MODEL_METHOD = "ELASTICIO_GET_MODEL_METHOD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_PASSWORD = "ELASTICIO_MESSAGE_CRYPTO_PASSWORD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_IV = "ELASTICIO_MESSAGE_CRYPTO_IV";
    public static final String ENV_VAR_AMQP_URI = "ELASTICIO_AMQP_URI";
    public static final String ENV_VAR_LISTEN_MESSAGES_ON = "ELASTICIO_LISTEN_MESSAGES_ON";
    public static final String ENV_VAR_PUBLISH_MESSAGES_TO = "ELASTICIO_PUBLISH_MESSAGES_TO";
    public static final String ENV_VAR_DATA_ROUTING_KEY = "ELASTICIO_DATA_ROUTING_KEY";
    public static final String ENV_VAR_ERROR_ROUTING_KEY = "ELASTICIO_ERROR_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_ROUTING_KEY = "ELASTICIO_REBOUND_ROUTING_KEY";
    public static final String ENV_VAR_SNAPSHOT_ROUTING_KEY = "ELASTICIO_SNAPSHOT_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_LIMIT = "ELASTICIO_REBOUND_LIMIT";
    public static final String ENV_VAR_REBOUND_INITIAL_EXPIRATION = "ELASTICIO_REBOUND_INITIAL_EXPIRATION";
    public static final String ENV_VAR_TASK = "ELASTICIO_TASK";
    public static final String ENV_VAR_STEP_ID = "ELASTICIO_STEP_ID";
    public static final String ENV_VAR_RABBITMQ_PREFETCH_SAILOR = "ELASTICIO_RABBITMQ_PREFETCH_SAILOR";

}
