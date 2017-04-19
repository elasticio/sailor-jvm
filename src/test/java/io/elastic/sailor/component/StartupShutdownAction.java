package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import io.elastic.api.Module;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class StartupShutdownAction implements Module {

    public static final String SUBSCRIPTION_ID = "my_startup_subscription_123456";

    private JsonObjectBuilder builder;

    @Override
    public JsonObject startup(JsonObject configuration) {
        builder = Json.createObjectBuilder()
                .add("startup", configuration);

        return Json.createObjectBuilder()
                .add("subscriptionId", SUBSCRIPTION_ID)
                .build();
    }

    public void execute(ExecutionParameters parameters) {
        final JsonObject body = Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .add("startupAndShutdown", builder.build())
                .build();


        final Message msg = new Message.Builder().body(body).build();

        parameters.getEventEmitter().emitData(msg);
    }
}
