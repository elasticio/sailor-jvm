package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.elastic.sailor.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SailorModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SailorModule.class.getName());

    @Override
    protected void configure() {

        bind(ApiClient.class).to(ApiClientImpl.class);

        bind(FunctionBuilder.class).to(FunctionBuilderImpl.class);
    }


    @Provides
    @Singleton
    @Named(Constants.NAME_STEP_JSON)
    Step provideTask(ApiClient apiClient, ContainerContext ctx) {

        return apiClient.retrieveFlowStep(ctx.getFlowId(), ctx.getStepId());
    }
}
