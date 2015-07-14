package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.JSON;

import java.util.Map;

public final class ServiceSettings {
    private final JsonObject envVars = new JsonObject();
    public JsonObject credentials;
    public String postResultUrl;
    public String selectModelMethod;
    public String actionOrTrigger;

    public ServiceSettings(Map<String, String> envVars) {
        validateAndParseVars(envVars);
    }

    public String getEnvVar(String name) {
        if (envVars.has(name)) {
            return envVars.get(name).getAsString();
        } else {
            throw new RuntimeException("No property with name " + name + " defined");
        }
    }

    private void validateAndParseVars(Map<String, String> envVars) {
        String err = "";
        for (Required key : Required.values()) {
            if (envVars.containsKey(key.name())) {
                key.parse(this, envVars.get(key.name()));
            } else {
                err += key.name() + " ";
            }
        }
        if (err.length() > 0) {
            throw new RuntimeException("Could not find properties: " + err);
        }
    }

    /**
     * POST_RESULT_URL - URL where to POST the result of execution (JSON)
     * CFG - JSON with user keys
     * ACTION_OR_TRIGGER - which action or trigger to execute
     * GET_MODEL_METHOD - name of the method to be called in Service.selectModel() function
     * See https://github.com/elasticio/sailor-nodejs/wiki/Service-verification for details
     */
    private enum Required {
        POST_RESULT_URL,
        CFG,
        ACTION_OR_TRIGGER,
        GET_MODEL_METHOD,
        COMPONENT_PATH;

        public void parse(ServiceSettings that, String value) {
            switch (this) {
                case POST_RESULT_URL: that.postResultUrl = value; break;
                case CFG: that.credentials = JSON.parse(value); break;
                case ACTION_OR_TRIGGER: that.actionOrTrigger = value; break;
                case GET_MODEL_METHOD: that.selectModelMethod = value; break;
            }
            that.envVars.addProperty(this.name(), value);
        }
    }
}
