package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.name.Names;
import io.elastic.api.EventEmitter;
import io.elastic.sailor.impl.*;

public class AmqpAwareModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(AmqpService.class).to(AmqpServiceImpl.class);
        bind(MessageProcessor.class).to(MessageProcessorImpl.class);
        bind(MessagePublisher.class).to(MessagePublisherImpl.class);
        bind(ErrorPublisher.class).to(ErrorPublisherImpl.class);
        bind(MessageResolver.class).to(MessageResolverImpl.class);


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
}
