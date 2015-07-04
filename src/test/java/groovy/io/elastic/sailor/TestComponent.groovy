package groovy.io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.api.Component
import io.elastic.api.EventEmitter
import io.elastic.api.ExecutionParameters

class TestComponent extends Component{

    public TestComponent(EventEmitter eventEmitter){
        super(eventEmitter);
    }

    public void execute(ExecutionParameters parameters){

        def snapshot = new JsonObject()
        snapshot.addProperty("lastUpdate", "2015-07-04")

        // emit received message back
        this.getEventEmitter().emitData(parameters.getMessage());
        this.getEventEmitter().emitSnapshot(snapshot);
        this.getEventEmitter().emitRebound("Please retry later");
        throw new Exception("Error happened in TestComponent!");
    }
}
