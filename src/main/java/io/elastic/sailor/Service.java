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
                   @Named(Constants.ENV_VAR_ACTION_OR_TRIGGER) String triggerOrAction,
                   @Named(Constants.ENV_VAR_GET_MODEL_METHOD) Provider<String> metaModelName) {
        this.postResultUrl = postResultUrl;

        final JsonElement triggerOrActionObj
                = resolver.findTriggerOrActionObject(triggerOrAction);

        params = new ServiceExecutionParameters.Builder()
                .configuration(configuration)
                .triggerOrAction(triggerOrActionObj.getAsJsonObject())
                .modelClassName(metaModelName.get())
                .build();
    }


    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            throw new IllegalArgumentException("3 arguments are required, but were passed " + args.length);
        }

        Injector injector = Guice.createInjector(new SailorModule(), new EnvironmentModule());

        final Service service = injector.getInstance(Service.class);

        final String methodName = args[2];

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
