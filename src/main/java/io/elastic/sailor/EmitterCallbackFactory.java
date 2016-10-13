package io.elastic.sailor;


import com.google.inject.name.Named;
import io.elastic.api.EventEmitter;

public interface EmitterCallbackFactory {

    @Named(Constants.NAME_CALLBACK_DATA)
    CountingCallback createDataCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_REBOUND)
    CountingCallback createReboundCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_ERROR)
    CountingCallback createErrorCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_SNAPSHOT)
    CountingCallback createSnapshotCallback(ExecutionContext context);

    @Named(Constants.NAME_CALLBACK_UPDATE_KEYS)
    EventEmitter.Callback createUpdateKeysCallback(ExecutionContext context);
}
