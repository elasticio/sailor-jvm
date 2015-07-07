package io.elastic.sailor;

public class Service {
    private final ComponentResolver component;

    public Service() {
        component = new ComponentResolver(System.getenv("COMPONENT_PATH"));
    }

    public void execService(Method method, Settings cfg, Parameters params) {
        try {
            getClass().getDeclaredMethod(method.name(), Settings.class, Parameters.class).invoke(this, cfg, params);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void verifyCredentials(Settings cfg, Parameters params) {
        try {
            Class.forName(component.loadVerifyCredentials())
                    .getDeclaredMethod("verifyCredentials")
                    .invoke(this, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getMetaModel(Settings cfg, Parameters params) {
        callModuleMethod(params.get("ACTION_OR_TRIGGER"), "getMetaModel", cfg);
    }

    private void selectModel(Settings cfg, Parameters params) {
        callModuleMethod(params.get("ACTION_OR_TRIGGER"), params.get("GET_MODEL_METHOD"), cfg);
    }

    private void callModuleMethod(String triggerOrActionName, String method, Settings cfg) {
        component.loadTriggerOrAction(triggerOrActionName);

    }

    public enum Method {
        verifyCredentials,
        getMetaModel,
        selectModel
    }
}
