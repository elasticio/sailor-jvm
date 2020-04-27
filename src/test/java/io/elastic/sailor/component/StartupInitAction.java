package io.elastic.sailor.component;

import io.elastic.api.*;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

public class StartupInitAction implements Function {

    private JsonObjectBuilder builder;

    @Override
    public JsonObject startup(final StartupParameters parameters) {
        builder = Json.createObjectBuilder()
                .add("startup", parameters.getConfiguration());
        return Json.createObjectBuilder().build();
    }

    @Override
    public void init(InitParameters parameters) {
        builder.add("init", parameters.getConfiguration());
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