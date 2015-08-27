package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.api.EventEmitter;
import io.elastic.sailor.impl.MessageProcessorImpl;

public class SailorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AMQPWrapperInterface.class).to(AMQPWrapper.class);
        bind(MessageProcessor.class).to(MessageProcessorImpl.class);

        install(new FactoryModuleBuilder()
                .implement(EventEmitter.Callback.class, Names.named("data"), DataCallback.class)
                .implement(EventEmitter.Callback.class, Names.named("error"), ErrorCallback.class)
                .implement(EventEmitter.Callback.class, Names.named("snapshot"), SnapshotCallback.class)
                .implement(EventEmitter.Callback.class, Names.named("rebound"), ReboundCallback.class)
                .build(EmitterCallbackFactory.class));
    }


    @Provides
    @Named("TaskJson")
    JsonObject provideTask(
            @Named(Constants.ENV_VAR_TASK) String task) {

        return new JsonParser().parse(task).getAsJsonObject();
    }
}
