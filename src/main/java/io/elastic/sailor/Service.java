package io.elastic.sailor;

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
                   @Named(Constants.NAME_CFG_JSON) JsonObject configuration,
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
                .credentialsVerifierClassName(resolver.findCredentialsVerifier())
                .build();
    }


    public static void main(String[] args) throws IOException {

        try {

            if (args.length < 1) {
                throw new IllegalArgumentException("1 argument is required, but were passed " + args.length);
            }

            Injector injector = Guice.createInjector(new ServiceModule(), new ServiceEnvironmentModule());

            final Service service = injector.getInstance(Service.class);

            final String methodName = args[0];

            logger.info("Starting execution of {}", methodName);

            final ServiceMethods method = ServiceMethods.parse(methodName);

            service.executeMethod(method);

        } catch (Exception e) {

            processException(e);
        }
    }

    public void executeMethod(final ServiceMethods method) {
        final JsonObject data = method.execute(this.params);

        JsonObject payload = new JsonObject();
        payload.addProperty("status", "success");
        payload.add("data", data);

        sendData(this.postResultUrl, payload);
    }

    private static void processException(Exception e) {

        JsonObject data = new JsonObject();
        data.addProperty("message", e.getMessage());

        JsonObject payload = new JsonObject();
        payload.addProperty("status", "success");
        payload.add("data", data);

        sendData(Utils.getOptionalEnvVar(Constants.ENV_VAR_POST_RESULT_URL), payload);
    }

    private static void sendData(String url, JsonObject payload) {

        logger.info("Sending response");

        if (url == null) {
            logger.info("URL is not provided");
            return;
        }

        try {
            String response = Utils.postJson(url, payload);
            logger.info("Received response from server: {}", response.toString());
        } catch (IOException e) {
            logger.info("Failed to send response: {}", e.getMessage());
        }
    }
}
