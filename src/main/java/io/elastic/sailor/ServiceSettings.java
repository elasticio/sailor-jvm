package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class ServiceSettings {

    public static final int DEFAULT_REBOUND_LIMIT = 20;
    public static final int DEFAULT_REBOUND_INITIAL_EXPIRATION = 15000;

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
    public static final String ENV_VAR_PUBLISH_MESSAGES_TO = "PUBLISH_MESSAGES_TO";
    public static final String ENV_VAR_DATA_ROUTING_KEY = "DATA_ROUTING_KEY";
    public static final String ENV_VAR_ERROR_ROUTING_KEY = "ERROR_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_ROUTING_KEY = "REBOUND_ROUTING_KEY";
    public static final String ENV_VAR_SNAPSHOT_ROUTING_KEY = "SNAPSHOT_ROUTING_KEY";
    public static final String ENV_VAR_REBOUND_LIMIT = "REBOUND_LIMIT";
    public static final String ENV_VAR_REBOUND_INITIAL_EXPIRATION = "REBOUND_INITIAL_EXPIRATION";
    public static final String ENV_VAR_TASK = "TASK";
    public static final String ENV_VAR_STEP_ID = "STEP_ID";

    public static String getStepId() {
        return Utils.getEnvVar(ServiceSettings.ENV_VAR_STEP_ID);
    }
}
