package io.elastic.sailor;

public final class Constants {

    public static final String NAME_STEP_JSON = "StepJson";
    public static final String NAME_CFG_JSON = "ConfigurationJson";
    public static final String NAME_CALLBACK_DATA = "dataCallback";
    public static final String NAME_CALLBACK_REBOUND = "reboundCallback";
    public static final String NAME_CALLBACK_ERROR = "errorCallback";
    public static final String NAME_CALLBACK_SNAPSHOT = "snapshotCallback";
    public static final String NAME_CALLBACK_UPDATE_KEYS = "updateKeys";
    public static final String NAME_HTTP_REPLY_KEYS = "httpReply";

    public static final String STEP_PROPERTY_ID = "id";
    public static final String STEP_PROPERTY_FUNCTION = "function";
    public static final String STEP_PROPERTY_COMP_ID = "comp_id";
    public static final String STEP_PROPERTY_CFG = "config";
    public static final String STEP_PROPERTY_SNAPSHOT = "snapshot";
    public static final String STEP_PROPERTY_PASSTHROUGH = "is_passthrough";

    public static final int DEFAULT_REBOUND_LIMIT = 20;
    public static final int DEFAULT_REBOUND_INITIAL_EXPIRATION = 15000;
    public static final int DEFAULT_RABBITMQ_PREFETCH_SAILOR = 1;

    public static final String ENV_VAR_ELASTICIO_PREFIX = "ELASTICIO_";
    public static final String ENV_VAR_API_URI = ENV_VAR_ELASTICIO_PREFIX + "API_URI";
    public static final String ENV_VAR_API_USERNAME = ENV_VAR_ELASTICIO_PREFIX + "API_USERNAME";
    public static final String ENV_VAR_API_KEY = ENV_VAR_ELASTICIO_PREFIX + "API_KEY";
    public static final String ENV_VAR_POST_RESULT_URL = ENV_VAR_ELASTICIO_PREFIX + "POST_RESULT_URL";
    public static final String ENV_VAR_CFG = ENV_VAR_ELASTICIO_PREFIX + "CFG";
    public static final String ENV_VAR_ACTION_OR_TRIGGER = ENV_VAR_ELASTICIO_PREFIX + "ACTION_OR_TRIGGER";
    public static final String ENV_VAR_FUNCTION = ENV_VAR_ELASTICIO_PREFIX + "FUNCTION";
    public static final String ENV_VAR_GET_MODEL_METHOD = ENV_VAR_ELASTICIO_PREFIX + "GET_MODEL_METHOD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_PASSWORD = ENV_VAR_ELASTICIO_PREFIX + "MESSAGE_CRYPTO_PASSWORD";
    public static final String ENV_VAR_MESSAGE_CRYPTO_IV = ENV_VAR_ELASTICIO_PREFIX + "MESSAGE_CRYPTO_IV";
    public static final String ENV_VAR_AMQP_URI = ENV_VAR_ELASTICIO_PREFIX + "AMQP_URI";
    public static final String ENV_VAR_LISTEN_MESSAGES_ON = ENV_VAR_ELASTICIO_PREFIX + "LISTEN_MESSAGES_ON";
    public static final String ENV_VAR_PUBLISH_MESSAGES_TO = ENV_VAR_ELASTICIO_PREFIX + "PUBLISH_MESSAGES_TO";
    public static final String ENV_VAR_DATA_ROUTING_KEY = ENV_VAR_ELASTICIO_PREFIX + "DATA_ROUTING_KEY";
    public static final String ENV_VAR_ERROR_ROUTING_KEY = ENV_VAR_ELASTICIO_PREFIX + "ERROR_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_ROUTING_KEY = ENV_VAR_ELASTICIO_PREFIX + "REBOUND_ROUTING_KEY";
    public static final String ENV_VAR_SNAPSHOT_ROUTING_KEY = ENV_VAR_ELASTICIO_PREFIX + "SNAPSHOT_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_LIMIT = ENV_VAR_ELASTICIO_PREFIX + "REBOUND_LIMIT";
    public static final String ENV_VAR_REBOUND_INITIAL_EXPIRATION = ENV_VAR_ELASTICIO_PREFIX + "REBOUND_INITIAL_EXPIRATION";
    public static final String ENV_VAR_FLOW_ID = ENV_VAR_ELASTICIO_PREFIX + "FLOW_ID";
    public static final String ENV_VAR_STEP_ID = ENV_VAR_ELASTICIO_PREFIX + "STEP_ID";
    public static final String ENV_VAR_EXEC_ID = ENV_VAR_ELASTICIO_PREFIX + "EXEC_ID";
    public static final String ENV_VAR_USER_ID = ENV_VAR_ELASTICIO_PREFIX + "USER_ID";
    public static final String ENV_VAR_COMP_ID = ENV_VAR_ELASTICIO_PREFIX + "COMP_ID";
    public static final String ENV_VAR_RABBITMQ_PREFETCH_SAILOR = ENV_VAR_ELASTICIO_PREFIX + "RABBITMQ_PREFETCH_SAILOR";
    public static final String ENV_VAR_STARTUP_REQUIRED = ENV_VAR_ELASTICIO_PREFIX + "STARTUP_REQUIRED";
    public static final String ENV_VAR_HOOK_SHUTDOWN = ENV_VAR_ELASTICIO_PREFIX + "HOOK_SHUTDOWN";

    public static final String AMQP_META_HEADER_PREFIX = "x-eio-meta-";
    public static final String AMQP_META_HEADER_TRACE_ID = Constants.AMQP_META_HEADER_PREFIX + "trace-id";

    public final static String AMQP_HEADER_MESSAGE_ID = "messageId";
    public final static String AMQP_HEADER_PARENT_MESSAGE_ID = "parentMessageId";
    public final static String AMQP_HEADER_REPLY_TO = "reply_to";
    public final static String AMQP_HEADER_EXEC_ID = "execId";
    public final static String AMQP_HEADER_TASK_ID = "taskId";
    public final static String AMQP_HEADER_USER_ID = "userId";
    public final static String AMQP_HEADER_STEP_ID = "stepId";
    public final static String AMQP_HEADER_COMPONENT_ID = "compId";
    public final static String AMQP_HEADER_FUNCTION = "function";
    public final static String AMQP_HEADER_START_TIMESTAMP = "start";
}
