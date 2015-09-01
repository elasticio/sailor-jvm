package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;


public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Named("ConfigurationJson")
    JsonObject provideConfiguration(
            @Named(Constants.ENV_VAR_CFG) String cfg) {

        return new JsonParser().parse(cfg).getAsJsonObject();
    }
}
