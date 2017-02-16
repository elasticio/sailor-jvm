package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import io.elastic.sailor.impl.HttpUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

public class Service {
    private static final Logger logger = LoggerFactory.getLogger(Service.class.getName());

    private final String postResultUrl;
    private final ServiceExecutionParameters params;

    @Inject()
    public Service(ComponentDescriptorResolver resolver,
                   @Named(Constants.ENV_VAR_POST_RESULT_URL) String postResultUrl,
                   @Named(Constants.NAME_CFG_JSON) JsonObject configuration,
                   @Named(Constants.ENV_VAR_ACTION_OR_TRIGGER) Provider<String> triggerOrActionProvider,
                   @Named(Constants.ENV_VAR_GET_MODEL_METHOD) Provider<String> metaModelName) {
        this.postResultUrl = postResultUrl;

        final String triggerOrAction = triggerOrActionProvider.get();


        JsonObject triggerOrActionObj = null;

        if (triggerOrAction != null) {
            triggerOrActionObj = resolver
                    .findModuleObject(triggerOrAction);
        }

        params = new ServiceExecutionParameters.Builder()
                .configuration(configuration)
                .triggerOrAction(triggerOrActionObj)
                .modelClassName(metaModelName.get())
                .credentialsVerifierClassName(resolver.findCredentialsVerifier())
                .build();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("1 argument is required, but were passed " + args.length);
        }

        Injector injector = Guice.createInjector(new ServiceModule(), new ServiceEnvironmentModule());

        final String methodName = args[0];

        logger.info("Starting execution of {}", methodName);

        final ServiceMethods method = ServiceMethods.parse(methodName);

        getServiceInstanceAndExecute(method, injector);

        logger.info("Java exiting with 0");

        System.exit(0);

    }

    public static void getServiceInstanceAndExecute(
            final ServiceMethods method, final Injector injector) {

        final Service service = injector.getInstance(Service.class);

        try {
            service.executeMethod(method);
        } catch (Exception e) {
            service.processException(e);

            throw new RuntimeException(e);
        }
    }

    private void createResponseAndSend(final String status,
                                       final JsonObject data) {

        final JsonObject payload = Json.createObjectBuilder()
                .add("status", status)
                .add("data", data)
                .build();

        sendData(this.postResultUrl, payload);
    }

    public void executeMethod(final ServiceMethods method) {
        final JsonObject data = method.execute(this.params);

        createResponseAndSend("success", data);
    }

    private void processException(Exception e) {

        e.printStackTrace();

        StringWriter writer = new StringWriter();
        e.printStackTrace(new PrintWriter(writer));

        final JsonObject data = Json.createObjectBuilder()
                .add("message", writer.toString())
                .build();

        createResponseAndSend("error", data);
    }

    private static void sendData(String url, JsonObject payload) {

        logger.info("Sending response");

        try {
            String response = HttpUtils.postJson(url, payload);
            logger.info("Received response from server: {}", response.toString());
        } catch (IOException e) {
            logger.info("Failed to send response: {}", e.getMessage());
        }
    }
}
