package io.elastic.sailor.component;

import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;

import javax.json.Json;
import javax.json.JsonObject;

public class HelloWorldAction implements Component {

    public void execute(ExecutionParameters parameters) {
        final JsonObject body = Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .build();

        final Message msg = new Message.Builder().body(body).build();

        parameters.getEventEmitter().emitData(msg);
    }
}
