package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

abstract class AbstractSailorTestModule extends AbstractModule {


    void bindRequiredStringEnvVar(final String name, final String value) {
        bind(String.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindRequiredIntegerEnvVar(final String name, final Integer value) {
        bind(Integer.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindRequiredLongEnvVar(final String name, final Long value) {
        bind(Long.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }

    void bindRequiredBooleanEnvVar(final String name, final Boolean value) {
        bind(Boolean.class)
                .annotatedWith(Names.named(name))
                .toInstance(value);
    }
}
