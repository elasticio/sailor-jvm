package io.elastic.sailor;

import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Component;
import io.elastic.api.Message;
import io.elastic.demo.*;

public class TaskExecutor {

    private static final int TIMEOUT = System.getenv("TIMEOUT")!=null && Integer.parseInt(System.getenv("TIMEOUT"))>0 ?
            Integer.parseInt(System.getenv("TIMEOUT")) : 20 * 60 * 1000;

    public TaskExecutor(String className) {

    }

    public void execute(ExecutionParameters params, EventEmitter emitter){

        // @TODO look for timeout

        try {
            Component component = new ErroneousComponent(emitter);
            component.execute(params);
        } catch (Exception e) {
            // @TODO emit error
            // @TODO emit end
        }

    }

    public static void main(String[] args) {
        TaskExecutor ex = new TaskExecutor("test");
        Message msg = new Message.Builder().build();
        ExecutionParameters params = new ExecutionParameters.Builder(msg).build();
        ex.execute(params);
    }

}
