package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import io.elastic.api.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;

public class HelloWorldAction implements Module {

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
