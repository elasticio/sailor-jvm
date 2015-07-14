package io.elastic.sailor.component

import io.elastic.api.Component
import io.elastic.api.EventEmitter
import io.elastic.api.ExecutionParameters

class SleepAction extends Component{

    public SleepAction(EventEmitter eventEmitter){
        super(eventEmitter);
    }

    public void execute(ExecutionParameters parameters){
        for (int i =0; i < 100; i++) {
            System.out.println('Iteration ' + i);
            Thread.sleep(100);
        }
        this.getEventEmitter().emitData(parameters.getMessage());
        throw new Exception("Error happened in SleepAction!");
    }
}
