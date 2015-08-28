package io.elastic.sailor;

import com.google.gson.JsonObject;

public class ServiceExecutionParameters {

    private final String className;
    private final JsonObject configuration;
    private final JsonObject triggerOrAction;
    private final String modelClassName;

    private ServiceExecutionParameters(
            final String componentClassName,
            final JsonObject configuration,
            final JsonObject triggerOrAction,
            final String modelClassName) {
        this.className = componentClassName;
        this.configuration = configuration;
        this.triggerOrAction = triggerOrAction;
        this.modelClassName = modelClassName;
    }

    public String getClassName() {
        return className;
    }

    public JsonObject getConfiguration() {
        return configuration;
    }

    public JsonObject getTriggerOrAction() {
        return triggerOrAction;
    }

    public String getModelClassName() {
        return modelClassName;
    }

    public static final class Builder {
        private String componentClassName;
        private JsonObject configuration;
        private JsonObject triggerOrAction;
        private String modelClassName;

        public Builder() {
            this.configuration = new JsonObject();
        }

        public Builder className(String value) {

            this.componentClassName = value;

            return this;
        }

        public Builder configuration(JsonObject configuration) {

            this.configuration = configuration;

            return this;
        }

        public Builder triggerOrAction(JsonObject value) {

            this.triggerOrAction = value;

            return this;
        }

        public Builder modelClassName(String value) {

            this.modelClassName = value;

            return this;
        }

        public ServiceExecutionParameters build() {
            return new ServiceExecutionParameters(
                    this.componentClassName,
                    this.configuration,
                    this.triggerOrAction,
                    this.modelClassName);
        }
    }
}
