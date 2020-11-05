package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.api.Function;
import io.elastic.sailor.impl.*;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class SailorModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(SailorModule.class.getName());

    @Override
    protected void configure() {

        bind(ApiClient.class).to(ApiClientImpl.class);

        bind(FunctionBuilder.class).to(FunctionBuilderImpl.class);

        final Manifest manifest = readManifest();

        bind(String.class)
                .annotatedWith(Names.named(Constants.NAME_SAILOR_VERSION))
                .toInstance(getSailorVersion(manifest));
    }


    @Provides
    @Singleton
    @Named(Constants.NAME_STEP_JSON)
    Step provideTask(ApiClient apiClient, ContainerContext ctx) {

        return apiClient.retrieveFlowStep(ctx.getFlowId(), ctx.getStepId());
    }

    @Provides
    @Singleton
    @Named(Constants.NAME_FUNCTION_OBJECT)
    Function provideFunction(final FunctionBuilder builder) {

        return builder.build();
    }

    private static final String getSailorVersion(final Manifest manifest) {

        if (manifest == null) {
            return "unknown";
        }

        final Attributes attributes = manifest.getMainAttributes();

        final String value = attributes.getValue("Implementation-Version");

        if (value == null) {
            return "unknown";
        }

        return value;
    }

    private static final Manifest readManifest() {

        final InputStream stream = SailorModule.class.getResourceAsStream("/META-INF/MANIFEST.MF");

        if (stream == null) {
            return null;
        }

        try {
            return new Manifest(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(stream);
        }
    }
}
