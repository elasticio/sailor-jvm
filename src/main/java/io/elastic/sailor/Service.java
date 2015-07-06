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

    }

    private void getMetaModel(Settings cfg, Parameters params) {

    }

    private void selectModel(Settings cfg, Parameters params) {

    }

    public enum Method {
        verifyCredentials,
        getMetaModel,
        selectModel
    }
}
