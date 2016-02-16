package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.sailor.impl.*;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SailorModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SailorModule.class.getName());

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
    @Named(Constants.NAME_STEP_JSON)
    Step provideTask(
            @Named(Constants.ENV_VAR_API_URI) String apiUri,
            @Named(Constants.ENV_VAR_API_USERNAME) String apiUser,
            @Named(Constants.ENV_VAR_API_KEY) String apiKey,
            @Named(Constants.ENV_VAR_TASK_ID) String taskId,
            @Named(Constants.ENV_VAR_STEP_ID) String stepId) {

        final String uri = String.format("%s/v1/tasks/%s/steps/%s", apiUri, taskId, stepId);

        logger.info("Retrieving step data for user {} at: {}", apiUser, uri);

        final UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(apiUser, apiKey);

        final JsonElement step = HttpUtils.getJson(uri, credentials);

        return new Step(step.getAsJsonObject());
    }
}
