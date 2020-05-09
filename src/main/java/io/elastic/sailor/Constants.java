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
    public static final int DEFAULT_API_REQUEST_RETRY_ATTEMPTS = 3;

    public static final String ENV_VAR_API_URI = "ELASTICIO_API_URI";
    public static final String ENV_VAR_API_USERNAME = "ELASTICIO_API_USERNAME";
    public static final String ENV_VAR_API_KEY = "ELASTICIO_API_KEY";
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
    public static final String ENV_VAR_FLOW_ID = "ELASTICIO_FLOW_ID";
    public static final String ENV_VAR_STEP_ID = "ELASTICIO_STEP_ID";
    public static final String ENV_VAR_EXEC_ID = "ELASTICIO_EXEC_ID";
    public static final String ENV_VAR_USER_ID = "ELASTICIO_USER_ID";
    public static final String ENV_VAR_COMP_ID = "ELASTICIO_COMP_ID";
    public static final String ENV_VAR_CONTAINER_ID = "ELASTICIO_CONTAINER_ID";
    public static final String ENV_VAR_COMP_NAME = "ELASTICIO_COMP_NAME";
    public static final String ENV_VAR_CONTRACT_ID = "ELASTICIO_CONTRACT_ID";
    public static final String ENV_VAR_EXEC_TYPE = "ELASTICIO_EXEC_TYPE";
    public static final String ENV_VAR_EXECUTION_RESULT_ID = "ELASTICIO_EXECUTION_RESULT_ID";
    public static final String ENV_VAR_FLOW_VERSION = "ELASTICIO_FLOW_VERSION";
    public static final String ENV_VAR_TASK_USER_EMAIL = "ELASTICIO_TASK_USER_EMAIL";
    public static final String ENV_VAR_TENANT_ID = "ELASTICIO_TENANT_ID";
    public static final String ENV_VAR_WORKSPACE_ID = "ELASTICIO_WORKSPACE_ID";
    public static final String ENV_VAR_RABBITMQ_PREFETCH_SAILOR = "ELASTICIO_RABBITMQ_PREFETCH_SAILOR";
    public static final String ENV_VAR_STARTUP_REQUIRED = "ELASTICIO_STARTUP_REQUIRED";
    public static final String ENV_VAR_HOOK_SHUTDOWN = "ELASTICIO_HOOK_SHUTDOWN";
    public static final String ENV_VAR_API_REQUEST_RETRY_ATTEMPTS =
            "ELASTICIO_API_REQUEST_RETRY_ATTEMPTS";
    public static final String ENV_VAR_NO_SELF_PASSTRHOUGH =
            "ELASTICIO_NO_SELF_PASSTRHOUGH";
    public static final String ENV_VAR_AMQP_PUBLISH_RETRY_ATTEMPTS = "ELASTICIO_AMQP_PUBLISH_RETRY_ATTEMPTS";
    public static final String ENV_VAR_AMQP_PUBLISH_RETRY_DELAY = "ELASTICIO_AMQP_PUBLISH_RETRY_DELAY";
    public static final String ENV_VAR_AMQP_PUBLISH_MAX_RETRY_DELAY = "ELASTICIO_AMQP_PUBLISH_MAX_RETRY_DELAY";

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
    public static final String AMQP_HEADER_THREAD_ID = "threadId";
    public static final String AMQP_HEADER_CONTAINER_ID = "containerId";

    public static final String MDC_THREAD_ID = AMQP_HEADER_THREAD_ID;
    public static final String MDC_MESSAGE_ID = AMQP_HEADER_MESSAGE_ID;
    public static final String MDC_PARENT_MESSAGE_ID = AMQP_HEADER_PARENT_MESSAGE_ID;
}
