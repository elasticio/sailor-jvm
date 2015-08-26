package io.elastic.sailor;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class.getName());


    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("3 arguments are required, but were passed " + args.length);
        }

        final String methodName = args[2];

        logger.info("Starting execution of " + methodName);

        final ServiceMethods method = ServiceMethods.parse(methodName);

        final JsonObject result = method.execute();

        logger.info("About to send response back");

        String response = Utils.postJson(ServiceSettings.getEnvVarPostResultUrl(), result);

        logger.info("Received response from server: {}", response.toString());
    }
}
