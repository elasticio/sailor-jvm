package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.elastic.api.Function;
import io.elastic.sailor.FunctionBuilder;
import io.elastic.sailor.ComponentDescriptorResolver;
import io.elastic.sailor.Constants;
import io.elastic.sailor.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class FunctionBuilderImpl implements FunctionBuilder {
    private static final Logger logger = LoggerFactory.getLogger(FunctionBuilderImpl.class);

    private final ComponentDescriptorResolver componentDescriptorResolver;
    private final Step step;

    @Inject
    public FunctionBuilderImpl(final ComponentDescriptorResolver componentDescriptorResolver,
                               @Named(Constants.NAME_STEP_JSON) final Step step) {
        this.componentDescriptorResolver = componentDescriptorResolver;
        this.step = step;
    }

    @Override
    public Function build() {

        final String module = step.getFunction();

        final String className = componentDescriptorResolver.findModule(module);

        logger.debug("Function Java class to be instantiated: {}", className);

        try {
            return newComponent(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Function newComponent(final String componentClassName) throws Exception {
        logger.debug("Instantiating component {}", componentClassName);

        final Class<?> clazz = Class.forName(componentClassName);

        return (Function) clazz.cast(clazz.newInstance());
    }
}
