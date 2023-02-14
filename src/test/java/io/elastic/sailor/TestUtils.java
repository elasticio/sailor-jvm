package io.elastic.sailor;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class TestUtils {

    public static Step createStep(final String functionName, final boolean putIncomingMessageIntoPassThrough) {
        final JsonObject step = Json.createObjectBuilder()
                .add("id", "step_1")
                .add("comp_id", "testcomponent")
                .add("function", functionName)
                .add("snapshot", Json.createObjectBuilder().add("timestamp", "19700101").build())
                .build();
        return new Step(step, putIncomingMessageIntoPassThrough);
    }

    public static Step createStep() {
        return TestUtils.createStep("test", false);
    }

    public static Step createStep(final String functionName) {
        return TestUtils.createStep(functionName, false);
    }
}
