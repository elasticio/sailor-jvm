package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.CredentialsVerifier;
import io.elastic.api.InvalidCredentialsException;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Service {
    private static final Logger logger = Logger.getLogger(Service.class.getName());

    private final ComponentResolver component;
    private final ServiceSettings settings;

    public Service(ServiceSettings serviceSettings) {
        settings = serviceSettings;
        component = new ComponentResolver(settings.getEnvVar("COMPONENT_PATH"));
    }

    public Service(Map<String, String> envVars) {
        settings = new ServiceSettings(envVars);
        component = new ComponentResolver(settings.getEnvVar("COMPONENT_PATH"));
    }

    public JsonObject execService(AvailableMethod method) {
        try {
            return (JsonObject)getClass().getDeclaredMethod(method.name()).invoke(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @return {'verified':true} in case there is no verification class field in component.json
     * or verification finished successfully, otherwise returns {'verified':false}
     */
    private JsonObject verifyCredentials() {
        JsonObject result = new JsonObject();
        result.addProperty("verified", true);
        try {
            CredentialsVerifier credentialsVerifier =
                (CredentialsVerifier)component.loadVerifyCredentials().newInstance();
            credentialsVerifier.verify(settings.credentials);
        } catch (Throwable e) {
            result.addProperty("verified", false);
            result.addProperty("reason", e.getMessage());
        }
        return result;
    }

    private JsonObject getMetaModel() {
        return callModuleMethod(
                settings.actionOrTrigger,
                AvailableMethod.getMetaModel.name()
        );
    }

    private JsonObject selectModel() {
        return callModuleMethod(
                settings.actionOrTrigger,
                settings.selectModelMethod
        );
    }

    @SuppressWarnings("unchecked")
    private JsonObject callModuleMethod(String triggerOrActionName, String calledMethod) {
        try {
            Class triggerOrActionClass = component.loadTriggerOrAction(triggerOrActionName);
            Method methodToCall = triggerOrActionClass.getDeclaredMethod(calledMethod, JsonObject.class);
            methodToCall.setAccessible(true);
            return (JsonObject)methodToCall.invoke(null, settings.credentials);
        } catch (Exception e) {
            throw new RuntimeException("Error processing trigger or action method " + calledMethod + " : " + e);
        }
    }

    public enum AvailableMethod {
        verifyCredentials,
        getMetaModel,
        selectModel
    }

    public static void main(String[] args) throws IOException {
        AvailableMethod method;
        try {
            method = AvailableMethod.valueOf(args[2]);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("No such method: " + args[2]);
        } catch (ArrayIndexOutOfBoundsException e) {
            throw new RuntimeException("Command line third argument should be present");
        } catch (NullPointerException e) {
            throw new RuntimeException("Command line third argument should be present");
        }
        ServiceSettings settings = new ServiceSettings(System.getenv());
        Service service = new Service(settings);
        //TODO: try to resend in case of failure <- other side is blocked without it
        logger.log(Level.INFO, "About to send resulting json");
        String response = Utils.postJson(settings.postResultUrl, service.execService(method));
        logger.log(Level.INFO, "Received response from server: " + response);
    }
}
