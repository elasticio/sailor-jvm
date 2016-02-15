package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.sailor.impl.*;

public class SailorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AMQPWrapperInterface.class).to(AMQPWrapper.class);
        bind(MessageProcessor.class).to(MessageProcessorImpl.class);

        install(new FactoryModuleBuilder()
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_DATA),
                        DataCallback.class)
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_ERROR),
                        ErrorCallback.class)
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_SNAPSHOT),
                        SnapshotCallback.class)
                .implement(
                        CountingCallback.class,
                        Names.named(Constants.NAME_CALLBACK_REBOUND),
                        ReboundCallback.class)
                .build(EmitterCallbackFactory.class));
    }


    @Provides
    @Named(Constants.NAME_TASK_JSON)
    JsonObject provideTask(
            @Named(Constants.ENV_VAR_API_URI) String apiUri) {

        final JsonElement task = Utils.getJson(apiUri);

        return task.getAsJsonObject();
    }

    @Provides
    @Named(Constants.NAME_CFG_JSON)
    JsonObject provideConfiguration(
            @Named(Constants.ENV_VAR_STEP_ID) String stepId,
            @Named(Constants.NAME_TASK_JSON) JsonObject task) {

        final JsonElement data = task.get("data");

        if (data == null) {
            throw new IllegalStateException("Property 'data' is missing in task's JSON");
        }

        final JsonElement stepData = data.getAsJsonObject().get(stepId);


        if (stepData == null) {
            throw new IllegalStateException("No configuration provided for step:" + stepId);
        }

        return stepData.getAsJsonObject();
    }
}
