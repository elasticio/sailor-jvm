package io.elastic.sailor;

import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;
import io.elastic.api.Component;
import io.elastic.api.EventEmitter.Callback;

import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

public class TaskExecutor {

    private Class COMPONENT_CLASS = io.elastic.api.Component.class;
    private Class EMITTER_CLASS = io.elastic.api.EventEmitter.class;

    private Class classToExecute;
    private int timeout = 20 * 60 * 1000; // 20 min
    private Callback errorCallback;
    private Callback dataCallback;
    private Callback snapshotCallback;
    private Callback reboundCallback;

    public TaskExecutor(String className) {
        classToExecute = findClass(className);
    }

    public void setTimeout(int msec) {
        timeout = msec;
    }

    public TaskExecutor onError(Callback callback) {
        errorCallback = callback;
        return this;
    }

    public TaskExecutor onData(Callback callback) {
        dataCallback = callback;
        return this;
    }

    public TaskExecutor onSnapshot(Callback callback) {
        snapshotCallback = callback;
        return this;
    }

    public TaskExecutor onRebound(Callback callback) {
        reboundCallback = callback;
        return this;
    }

    private Class findClass(String className){
        try {
            return Class.forName(className).asSubclass(COMPONENT_CLASS);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class " + className + " is not found");
        } catch (ClassCastException e) {
            throw new RuntimeException("Class " + className + " is not " + COMPONENT_CLASS.getCanonicalName());
        }
    }

    public void execute(final ExecutionParameters params){

        final EventEmitter emitter = new EventEmitter.Builder()
                .onData(dataCallback)
                .onError(errorCallback)
                .onRebound(reboundCallback)
                .onSnapshot(snapshotCallback)
                .build();

        try {
            final Object object = classToExecute.getConstructor(EMITTER_CLASS).newInstance(emitter);
            final Component component = (Component)object;
            final Thread thread = new Thread() {
                @Override
                public void run() {
                    try {
                        component.execute(params);
                    } catch (Exception e) {
                        errorCallback.receive(e);
                    }
                }
            };
            runWithTimeout(thread);
        } catch (Exception e) {
            this.errorCallback.receive(e);
        }
    }

    private void runWithTimeout(Runnable thread){
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Future future = executor.submit(thread);
        executor.shutdown();
        try {
            future.get(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException ie) {
            throw new RuntimeException(ie.getMessage());
        }
        catch (TimeoutException te) {
            throw new RuntimeException("Processing time out - " + classToExecute.getCanonicalName());
        }
    }
}
