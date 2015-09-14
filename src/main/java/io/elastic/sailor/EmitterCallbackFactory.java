package io.elastic.sailor;


import com.google.inject.name.Named;

public interface EmitterCallbackFactory {

    @Named(Constants.NAME_CALLBACK_DATA)
    CountingCallback createDataCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_REBOUND)
    CountingCallback createReboundCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_ERROR)
    CountingCallback createErrorCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_SNAPSHOT)
    CountingCallback createSnapshotCallback(ExecutionContext context);
}
