package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;
import io.elastic.api.Module;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class StartupInitAction implements Module {

    private JsonObjectBuilder builder;

    @Override
    public JsonObject startup(JsonObject configuration) {
        builder = Json.createObjectBuilder()
                .add("startup", configuration);
        return Json.createObjectBuilder().build();
    }

    @Override
    public void init(JsonObject configuration) {
        builder.add("init", configuration);
    }

    public void execute(ExecutionParameters parameters) {
        final JsonObject body =Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .add("startupAndInit", builder.build())
                .build();


        final Message msg = new Message.Builder().body(body).build();

        parameters.getEventEmitter().emitData(msg);
    }
}
