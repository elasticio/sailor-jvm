package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.elastic.api.DynamicMetadataProvider;
import io.elastic.api.SelectModelProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

public enum ServiceMethods {

    verifyCredentials() {
        @Override
        JsonObject execute(final ServiceExecutionParameters params) {
            logger.info("About to verify credentials");

            JsonObject result = new JsonObject();
            result.addProperty("verified", true);

            return result;
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
        final JsonElement element = triggerOrAction.get(name);

        if (element == null) {
            throw new IllegalStateException(name + " is required");
        }

        final String className = element.getAsString();

        return newInstance(className);
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
