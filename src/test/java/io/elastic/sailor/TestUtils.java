package io.elastic.sailor;

import javax.json.Json;
import javax.json.JsonObject;

public class TestUtils {

    public static Step createStep() {
        final JsonObject step = Json.createObjectBuilder()
                .add("id", "step_1")
                .add("comp_id", "testcomponent")
                .add("function", "test")
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .build();
        return new Step(step);
    }
}
