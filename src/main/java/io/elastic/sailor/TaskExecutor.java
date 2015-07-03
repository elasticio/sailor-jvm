package io.elastic.sailor;

import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Component;
import io.elastic.api.Message;

import java.lang.reflect.Constructor;

public class TaskExecutor {

    private String className;

    private static final int TIMEOUT = System.getenv("TIMEOUT")!=null && Integer.parseInt(System.getenv("TIMEOUT"))>0 ?
            Integer.parseInt(System.getenv("TIMEOUT")) : 20 * 60 * 1000;

    public TaskExecutor(String className) {
        this.className = className;
    }

    public void execute(ExecutionParameters params, EventEmitter eventEmitter){

        // @TODO look for timeout
        System.out.println(className);
        try {
            Class<?> c = Class.forName(className);
            Constructor<?> cons = c.getConstructor(EventEmitter.class);
            Object object = cons.newInstance(eventEmitter);
            System.out.println(className);
            Component component = (Component)object;
            component.execute(params);
        } catch (ClassNotFoundException e) {
            System.out.println("Class " + className + " is not found!");
            // @TODO emit error
            // @TODO emit end
        } catch (NoSuchMethodException e) {
            System.out.println("Valid constructor in " + className + " is not found!");
            // @TODO emit error
            // @TODO emit end
        } catch (Exception e) {
            System.out.println("Failed to execute " + className);
            // @TODO emit error
            // @TODO emit end
        }
    }

    // testing
    public static void main(String[] args) {

        Message msg = new Message.Builder().build();
        ExecutionParameters params = new ExecutionParameters.Builder(msg).build();

        TaskExecutor ex = new TaskExecutor("io.elastic.demo.ErroneousComponent");
        EventEmitter eventEmitter = new EventEmitter.Builder()
                .onData(ex.getDataCallback())
                .onError(ex.getErrorCallback())
                .onRebound(ex.getReboundCallback())
                .onSnapshot(ex.getSnapshotCallback())
                .build();
        ex.execute(params, eventEmitter);

        TaskExecutor ex2 = new TaskExecutor("io.elastic.demo.HelloWorldComponent");
        ex2.execute(params, eventEmitter);

        TaskExecutor ex3 = new TaskExecutor("io.elastic.api.Message");
        ex3.execute(params, eventEmitter);
    }

    // data
    private EventEmitter.Callback getDataCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                System.out.println("newDataCallback" + data.toString());
                // @TODO process data and send to channel
            }
        };
    }

    private EventEmitter.Callback getErrorCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object err) {
                System.out.println("newErrorCallback" + err.toString());
                // @TODO process error and send to channel
            }
        };
    }

    private EventEmitter.Callback getReboundCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                System.out.println("newReboundCallback" + data.toString());
                // @TODO process rebound and send to channel
            }
        };
    }

    private EventEmitter.Callback getSnapshotCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object data) {
                System.out.println("newSnapshotCallback" + data.toString());
                // @TODO process snapshot and send to channel
            }
        };
    }

    private EventEmitter.Callback getEndCallback() {
        return new EventEmitter.Callback() {
            @Override
            public void receive(Object err) {
                System.out.println("newEndCallback");
                // @TODO process end and send to channel
            }
        };
    }

}
