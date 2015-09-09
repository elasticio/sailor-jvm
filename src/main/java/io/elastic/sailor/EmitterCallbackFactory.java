package io.elastic.sailor;


import com.google.inject.name.Named;

public interface EmitterCallbackFactory {

    @Named("data")
    CountingCallback createDataCallback(ExecutionContext context);

    @Named("rebound")
    CountingCallback createReboundCallback(ExecutionContext context);

    @Named("error")
    CountingCallback createErrorCallback(ExecutionContext context);

    @Named("snapshot")
    CountingCallback createSnapshotCallback(ExecutionContext context);
}
