package io.elastic.sailor;

import io.elastic.api.CredentialsVerifier;
import io.elastic.api.DynamicMetadataProvider;
import io.elastic.api.InvalidCredentialsException;
import io.elastic.api.SelectModelProvider;
import javax.json.JsonObjectBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import java.lang.reflect.Constructor;

public enum ServiceMethods {

    verifyCredentials() {
        @Override
        JsonObject execute(final ServiceExecutionParameters params) {
            logger.info("About to verify credentials");

            final String verifierClassName
                    = params.getCredentialsVerifierClassName();

            if (verifierClassName == null) {
                logger.info("No implementation of {} found",
                        CredentialsVerifier.class.getName());
                return createResult(true, null);

            }

            final CredentialsVerifier verifier = newInstance(verifierClassName);

            try {
                verifier.verify(params.getConfiguration());
                return createResult(true, null);
            } catch (InvalidCredentialsException e) {
                return createResult(false, e.getMessage());
            }
        }

        private JsonObject createResult(boolean verified, String errMessage) {
            JsonObjectBuilder result = Json.createObjectBuilder()
                .add("verified", verified);
            if (errMessage != null){
                result.add("reason", errMessage);
            }
            return result.build();
        }
    },

    getMetaModel() {
        @Override
        JsonObject execute(final ServiceExecutionParameters params) {

            final DynamicMetadataProvider provider
                    = newInstance(params.getTriggerOrAction(), "dynamicMetadata");

            return provider.getMetaModel(params.getConfiguration());
        }
    },

    selectModel() {
        @Override
        JsonObject execute(final ServiceExecutionParameters params) {

            final SelectModelProvider provider
                    = newInstance(params.getModelClassName());

            return provider.getSelectModel(params.getConfiguration());
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(ServiceMethods.class.getName());

    abstract JsonObject execute(ServiceExecutionParameters params);

    public static ServiceMethods parse(final String input) {
        if (input == null) {
            throw new IllegalArgumentException("Failed to parse a service method from null input");
        }

        for (ServiceMethods next : ServiceMethods.values()) {
            if (next.name().equalsIgnoreCase(input)) {
                return next;
            }
        }

        throw new IllegalStateException("Failed to parse a service method from input:" + input);
    }

    private static <T> T newInstance(final JsonObject triggerOrAction, final String name) {
        if (triggerOrAction == null) {
            throw new IllegalStateException(String.format(
                    "Env var '%s' is required", Constants.ENV_VAR_ACTION_OR_TRIGGER));
        }

        final JsonString className = triggerOrAction.getJsonString(name);

        if (className == null) {
            throw new IllegalStateException(name + " is required");
        }

        return newInstance(className.getString());
    }

    private static <T> T newInstance(String className) {
        logger.info("Instantiating class {}", className);
        try {
            final Class<?> clazz = Class.forName(className);

            final Constructor<?> constructor = clazz.getDeclaredConstructor();

            return (T) clazz.cast(constructor.newInstance());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
