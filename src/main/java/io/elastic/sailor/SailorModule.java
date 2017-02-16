package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.api.EventEmitter;
import io.elastic.sailor.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SailorModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SailorModule.class.getName());

    @Override
    protected void configure() {
        bind(AmqpService.class).to(AmqpServiceImpl.class);
        bind(MessageProcessor.class).to(MessageProcessorImpl.class);

        bind(ApiClient.class).to(ApiClientImpl.class);

        bind(ModuleBuilder.class).to(ModuleBuilderImpl.class);

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
                .implement(
                        EventEmitter.Callback.class,
                        Names.named(Constants.NAME_CALLBACK_UPDATE_KEYS),
                        UpdateKeysCallback.class)
                .implement(
                        EventEmitter.Callback.class,
                        Names.named(Constants.NAME_HTTP_REPLY_KEYS),
                        HttpReplyCallback.class)
                .build(EmitterCallbackFactory.class));
    }


    @Provides
    @Singleton
    @Named(Constants.NAME_STEP_JSON)
    Step provideTask(ApiClient apiClient, ContainerContext ctx) {

        return apiClient.retrieveFlowStep(ctx.getFlowId(), ctx.getStepId());
    }
}
