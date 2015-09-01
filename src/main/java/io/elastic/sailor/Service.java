package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class.getName());

    private final String postResultUrl;
    private final ServiceExecutionParameters params;

    @Inject()
    public Service(ComponentResolver resolver,
                   @Named(Constants.ENV_VAR_POST_RESULT_URL) String postResultUrl,
                   @Named("ConfigurationJson") JsonObject configuration,
                   @Named(Constants.ENV_VAR_ACTION_OR_TRIGGER) Provider<String> triggerOrActionProvider,
                   @Named(Constants.ENV_VAR_GET_MODEL_METHOD) Provider<String> metaModelName) {
        this.postResultUrl = postResultUrl;

        final String triggerOrAction = triggerOrActionProvider.get();


        JsonObject triggerOrActionObj  = null;

        if (triggerOrAction != null) {
            triggerOrActionObj = resolver
                    .findTriggerOrActionObject(triggerOrAction)
                    .getAsJsonObject();
        }

        params = new ServiceExecutionParameters.Builder()
                .configuration(configuration)
                .triggerOrAction(triggerOrActionObj)
                .modelClassName(metaModelName.get())
                .build();
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("1 argument is required, but were passed " + args.length);
        }

        Injector injector = Guice.createInjector(new ServiceModule(), new ServiceEnvironmentModule());

        final Service service = injector.getInstance(Service.class);

        final String methodName = args[0];

        logger.info("Starting execution of {}", methodName);

        final ServiceMethods method = ServiceMethods.parse(methodName);

        service.start(method);

    }

    public void start(final ServiceMethods method) throws IOException {

        final JsonObject result = method.execute(this.params);

        logger.info("Sending response");

        String response = Utils.postJson(this.postResultUrl, result);

        logger.info("Received response from server: {}", response.toString());
    }
}
