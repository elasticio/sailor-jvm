package io.elastic.sailor.component;

import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;

import javax.json.Json;
import javax.json.JsonObject;

public class TestAction extends Component {

    public TestAction(EventEmitter eventEmitter) {
        super(eventEmitter);
    }

    public void execute(ExecutionParameters parameters) {

        JsonObject snapshot = Json.createObjectBuilder()
                .add("lastUpdate", "2015-07-04")
                .build();

        // emit received message back
        this.getEventEmitter().emitData(parameters.getMessage());
        this.getEventEmitter().emitSnapshot(snapshot);
        this.getEventEmitter().emitRebound("Please retry later");
        throw new RuntimeException("Error happened in TestAction!");
    }
}
