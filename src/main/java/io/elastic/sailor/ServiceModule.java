package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.name.Named;
import io.elastic.api.JSON;

import jakarta.json.JsonObject;


public class ServiceModule extends AbstractModule {

    @Override
    protected void configure() {
    }

    @Provides
    @Named(Constants.NAME_CFG_JSON)
    JsonObject provideConfiguration(
            @Named(Constants.ENV_VAR_CFG) String cfg) {

        return JSON.parseObject(cfg);
    }
}
