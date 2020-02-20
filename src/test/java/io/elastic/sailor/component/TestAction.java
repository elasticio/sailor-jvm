package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.Function;

import javax.json.Json;
import javax.json.JsonObject;

public class TestAction implements Function {

    public void execute(ExecutionParameters parameters) {

        JsonObject snapshot = Json.createObjectBuilder()
                .add("lastUpdate", "2015-07-04")
                .build();

        // emit received message back
        parameters.getEventEmitter().emitData(parameters.getMessage());
        parameters.getEventEmitter().emitSnapshot(snapshot);
        parameters.getEventEmitter().emitRebound("Please retry later");
        throw new RuntimeException("Error happened in TestAction!");
    }
}
