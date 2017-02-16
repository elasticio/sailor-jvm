package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import io.elastic.api.Module;

import javax.json.Json;
import javax.json.JsonObject;

public class HelloWorldAction implements Module {

    public void execute(ExecutionParameters parameters) {
        final JsonObject body = Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .build();

        final Message msg = new Message.Builder().body(body).build();

        parameters.getEventEmitter().emitData(msg);
    }
}
