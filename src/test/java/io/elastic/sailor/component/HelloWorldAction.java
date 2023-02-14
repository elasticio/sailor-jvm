package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.Function;
import io.elastic.api.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class HelloWorldAction implements Function {

    private static final Logger logger = LoggerFactory.getLogger(HelloWorldAction.class);

    public void execute(final ExecutionParameters parameters) {
        logger.info("Starting execution");

        final JsonObject body = Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .build();

        final Message msg = new Message.Builder().body(body).build();

        logger.info("Emitting message");

        parameters.getEventEmitter().emitData(msg);

        logger.info("Finished execution");
    }
}
