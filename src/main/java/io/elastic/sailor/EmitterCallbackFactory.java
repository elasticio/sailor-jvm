package io.elastic.sailor;


import com.google.inject.name.Named;
import io.elastic.api.EventEmitter;

public interface EmitterCallbackFactory {

    @Named("data")
    EventEmitter.Callback createDataCallback(ExecutionContext context);

    @Named("rebound")
    EventEmitter.Callback createReboundCallback(ExecutionContext context);

    @Named("error")
    EventEmitter.Callback createErrorCallback(ExecutionContext context);

    @Named("snapshot")
    EventEmitter.Callback createSnapshotCallback(ExecutionContext context);
}
