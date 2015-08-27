package io.elastic.sailor;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ServiceMethods {

    verifyCredentials() {
        @Override
        JsonObject execute() {
            logger.info("About to verify credentials");

            JsonObject result = new JsonObject();
            result.addProperty("verified", true);

            return result;
        }
    },

    getMetaModel() {
        @Override
        JsonObject execute() {
            logger.info("About to retrieve meta model");
            return new JsonObject();
        }
    },

    selectModel() {
        @Override
        JsonObject execute() {
            logger.info("About to retrieve select model");

            return new JsonObject();
        }
    };

    private static final Logger logger = LoggerFactory.getLogger(ServiceMethods.class.getName());

    abstract JsonObject execute();

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
}
