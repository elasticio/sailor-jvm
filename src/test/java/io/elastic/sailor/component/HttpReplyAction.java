package io.elastic.sailor.component;

import io.elastic.api.ExecutionParameters;
import io.elastic.api.HttpReply;
import io.elastic.api.JSON;
import io.elastic.api.Module;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;

public class HttpReplyAction implements Module {

    public void execute(ExecutionParameters parameters) {
        final JsonObject body =Json.createObjectBuilder()
                .add("echo", parameters.getMessage().getBody())
                .build();

        final HttpReply httpReply = new HttpReply.Builder()
                .header("Content-type", "application/json")
                .header("x-custom-header", "abcdef")
                .status(HttpReply.Status.ACCEPTED)
                .content(new ByteArrayInputStream(JSON.stringify(body).getBytes()))
                .build();

        parameters.getEventEmitter().emitHttpReply(httpReply);
    }
}
