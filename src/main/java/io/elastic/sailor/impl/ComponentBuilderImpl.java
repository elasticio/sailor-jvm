package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.sailor.ComponentBuilder;
import io.elastic.sailor.ComponentResolver;
import io.elastic.sailor.Constants;
import io.elastic.sailor.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;

public class ComponentBuilderImpl implements ComponentBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ComponentBuilderImpl.class);

    private final ComponentResolver componentResolver;
    private final Step step;

    @Inject
    public ComponentBuilderImpl(final ComponentResolver componentResolver,
                                @Named(Constants.NAME_STEP_JSON) final Step step) {
        this.componentResolver = componentResolver;
        this.step = step;
    }

    @Override
    public Component build() {

        final String triggerOrAction = step.getFunction();

        final String className = componentResolver.findTriggerOrAction(triggerOrAction);

        logger.info("Component Java class to be instantiated: {}", className);

        try {
            return newComponent(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Component newComponent(final String componentClassName) throws Exception {
        logger.info("Instantiating component {}", componentClassName);

        final Class<?> clazz = Class.forName(componentClassName);

        return (Component) clazz.cast(clazz.newInstance());
    }
}
