package io.elastic.sailor.component;

import com.google.gson.JsonObject;
import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;

public class TestAction extends Component {

    public TestAction(EventEmitter eventEmitter) {
        super(eventEmitter);
    }

    public void execute(ExecutionParameters parameters) {

        JsonObject snapshot = new JsonObject();
        snapshot.addProperty("lastUpdate", "2015-07-04");

        // emit received message back
        this.getEventEmitter().emitData(parameters.getMessage());
        this.getEventEmitter().emitSnapshot(snapshot);
        this.getEventEmitter().emitRebound("Please retry later");
        throw new RuntimeException("Error happened in TestAction!");
    }
}
