package io.elastic.sailor;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.google.inject.name.Names;
import io.elastic.sailor.impl.ApiClientImpl;
import io.elastic.sailor.impl.HttpUtils;
import org.apache.http.impl.client.CloseableHttpClient;

public class HttpClientSimulationTestModule extends AbstractSailorTestModule {

    @Override
    protected void configure() {
        bind(ApiClient.class).to(ApiClientImpl.class);
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_URI, "http://localhost:8089");
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_USERNAME, "test");
        bindRequiredStringEnvVar(Constants.ENV_VAR_API_KEY, "test");
        bind(Boolean.class)
                .annotatedWith(Names.named(Constants.ENV_VAR_NO_SELF_PASSTRHOUGH))
                .toInstance(false);
        bindRequiredIntegerEnvVar(Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS, 3);
    }

    @Provides
    @Singleton
    CloseableHttpClient provideHttpClient(@Named(Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS) final int retryCount) {
        return HttpUtils.createHttpClient(retryCount);
    }
}
