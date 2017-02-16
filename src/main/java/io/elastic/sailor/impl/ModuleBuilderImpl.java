package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.elastic.api.Module;
import io.elastic.sailor.ModuleBuilder;
import io.elastic.sailor.ComponentDescriptorResolver;
import io.elastic.sailor.Constants;
import io.elastic.sailor.Step;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class ModuleBuilderImpl implements ModuleBuilder {
    private static final Logger logger = LoggerFactory.getLogger(ModuleBuilderImpl.class);

    private final ComponentDescriptorResolver componentDescriptorResolver;
    private final Step step;

    @Inject
    public ModuleBuilderImpl(final ComponentDescriptorResolver componentDescriptorResolver,
                             @Named(Constants.NAME_STEP_JSON) final Step step) {
        this.componentDescriptorResolver = componentDescriptorResolver;
        this.step = step;
    }

    @Override
    public Module build() {

        final String module = step.getFunction();

        final String className = componentDescriptorResolver.findModule(module);

        logger.info("Module Java class to be instantiated: {}", className);

        try {
            return newComponent(className);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private Module newComponent(final String componentClassName) throws Exception {
        logger.info("Instantiating component {}", componentClassName);

        final Class<?> clazz = Class.forName(componentClassName);

        return (Module) clazz.cast(clazz.newInstance());
    }
}
