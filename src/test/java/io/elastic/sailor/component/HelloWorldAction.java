package io.elastic.sailor.component;

import com.google.gson.JsonObject;
import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Message;

public class HelloWorldAction extends Component {

    public HelloWorldAction(EventEmitter eventEmitter) {
        super(eventEmitter);
    }

    public void execute(ExecutionParameters parameters) {

        JsonObject body = new JsonObject();
        body.addProperty("de", "Hallo, Welt!");
        body.addProperty("en", "Hello, world!");

        new Message.Builder().body(body).build();

        this.getEventEmitter().emitData(parameters.getMessage());
    }
}
