package io.elastic.sailor;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.elastic.sailor.impl.HttpUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpClientModule extends AbstractModule {

    private static final Logger logger = LoggerFactory.getLogger(HttpClientModule.class);

    @Override
    protected void configure() {
    }

    @Provides
    @Singleton
    CloseableHttpClient provideHttpClient(@Named(Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS) final int retryCount) {
        logger.debug("Creating new singleton HTTP client");
        return HttpUtils.createHttpClient(retryCount);
    }
}
