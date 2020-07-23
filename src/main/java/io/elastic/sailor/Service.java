package io.elastic.sailor;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
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

    private ComponentDescriptorResolver resolver;
    private String postResultUrl;
    private JsonObject configuration;
    private String triggerOrAction;
    private String metaModelName;
    private int retryCount;

    protected ServiceExecutionParameters createServiceExecutionParameters() {

        JsonObject triggerOrActionObj = null;

        if (this.triggerOrAction != null) {
            triggerOrActionObj = resolver
                    .findModuleObject(this.triggerOrAction);
        }

        return new ServiceExecutionParameters.Builder()
                .configuration(this.configuration)
                .triggerOrAction(triggerOrActionObj)
                .modelClassName(this.metaModelName)
                .credentialsVerifierClassName(resolver.findCredentialsVerifier())
                .build();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException("1 argument is required, but were passed " + args.length);
        }

        createServiceInstanceAndExecute(args[0]);

        logger.info("Java exiting with 0");

        System.exit(0);

    }

    static void createServiceInstanceAndExecute(final String methodName) {
        Injector injector = Guice.createInjector(new ServiceModule(), new ServiceEnvironmentModule());

        logger.info("Starting execution of {}", methodName);

        final ServiceMethods method = ServiceMethods.parse(methodName);

        getServiceInstanceAndExecute(method, injector);
    }

    public static void getServiceInstanceAndExecute(
            final ServiceMethods method, final Injector injector) {

        final Service service = injector.getInstance(Service.class);
        final ServiceExecutionParameters params = service.createServiceExecutionParameters();

        try {
            service.executeMethod(method, params);
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

        sendData(this.postResultUrl, payload, this.retryCount);
    }

    public void executeMethod(final ServiceMethods method, final ServiceExecutionParameters params) {
        final JsonObject data = method.execute(params);

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

    private static void sendData(String url, JsonObject payload, int retryCnt) {

        logger.info("Sending response");

        String response = HttpUtils.postJson(url, payload, new HttpUtils.BasicURLAuthorizationHandler(), retryCnt);

        logger.info("Received response from server: {}", response.toString());
    }

    @Inject
    public void setResolver(final ComponentDescriptorResolver resolver) {
        this.resolver = resolver;
    }

    @Inject
    public void setPostResultUrl(@Named(Constants.ENV_VAR_POST_RESULT_URL) final String postResultUrl) {
        this.postResultUrl = postResultUrl;
    }

    @Inject
    public void setConfiguration(@Named(Constants.NAME_CFG_JSON) final JsonObject configuration) {
        this.configuration = configuration;
    }

    @Inject(optional = true)
    public void setTriggerOrAction(@Named(Constants.ENV_VAR_ACTION_OR_TRIGGER) final String triggerOrAction) {
        this.triggerOrAction = triggerOrAction;
    }

    @Inject(optional = true)
    public void setMetaModelName(@Named(Constants.ENV_VAR_GET_MODEL_METHOD) final String metaModelName) {
        this.metaModelName = metaModelName;
    }
    @Inject
    public void setRetryCount(@Named(Constants.ENV_VAR_API_REQUEST_RETRY_ATTEMPTS) final int retryCount) {
        this.retryCount = retryCount;
    }
}
