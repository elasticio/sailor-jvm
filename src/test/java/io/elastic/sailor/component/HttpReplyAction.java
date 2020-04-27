package io.elastic.sailor.component;

import io.elastic.api.*;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.ByteArrayInputStream;

public class HttpReplyAction implements Function {

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
