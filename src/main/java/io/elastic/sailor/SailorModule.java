package io.elastic.sailor;

import com.google.inject.AbstractModule;

public class SailorModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(AMQPWrapperInterface.class).to(AMQPWrapper.class);
    }
}
