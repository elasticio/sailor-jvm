package io.elastic.sailor

import com.google.gson.JsonObject

class ServiceSpec extends SetupServerHelper {

    def settings = new HashMap<String, String>(){{
        put("POST_RESULT_URL", "http://localhost:10000");
        put("ACTION_OR_TRIGGER", "test");
        put("CFG", "{\"key\":0}");
        put("GET_MODEL_METHOD", "getModel");
        put("COMPONENT_PATH", "src/test/java/io/elastic/sailor/component");
    }};

    def getHandler() {
        new SimpleRequestHandler()
    }

    def "it should verify credentials"() {
        given:
        Service.AvailableMethod method = Service.AvailableMethod.verifyCredentials;
        ServiceSettings serviceSettings = new ServiceSettings(settings);
        Service service = new Service(serviceSettings);
        def success = new JsonObject();
        success.addProperty("verified", true)
        when:
        Utils.postJson(serviceSettings.postResultUrl, service.execService(method));
        then:
        SimpleRequestHandler.lastMessage.contains(success.toString())
    }

    def "it should get meta model"() {
        given:
        Service.AvailableMethod method = Service.AvailableMethod.getMetaModel;
        ServiceSettings serviceSettings = new ServiceSettings(settings);
        Service service = new Service(serviceSettings);
        def success = new JsonObject();
        success.addProperty("key", 0)
        when:
        Utils.postJson(serviceSettings.postResultUrl, service.execService(method));
        then:
        SimpleRequestHandler.lastMessage.contains(success.toString())
    }

    def "it should get select model"() {
        given:
        Service.AvailableMethod method = Service.AvailableMethod.selectModel;
        ServiceSettings serviceSettings = new ServiceSettings(settings);
        Service service = new Service(serviceSettings);
        def success = new JsonObject();
        success.addProperty("key", 0)
        when:
        Utils.postJson(serviceSettings.postResultUrl, service.execService(method));
        then:
        SimpleRequestHandler.lastMessage.contains(success.toString())
    }

    def "ServiceSettings should throw an error when there are parameters missing or malformed"() {
        given:
        def badSettings = new HashMap<String, String>();
        badSettings.put("POST_RESULT_URL", "http://localhost");
        badSettings.put("ACTION_OR_TRIGGER", "action");
        badSettings.put("CFG", "{\"property\":0}");
        when:
        new ServiceSettings(badSettings);
        then:
        thrown(RuntimeException)
    }

    def "ServiceSettings should not throw an error when there are no parameters missing or malformed"() {
        when:
        new ServiceSettings(settings);

        then:
        notThrown(RuntimeException)
    }

    def "Service should not fail if component.json does not have required auth class field"() {
        given:
        settings.put("COMPONENT_PATH", "src/test/java/io/elastic/sailor/malformedcomponent");
        Service.AvailableMethod method = Service.AvailableMethod.verifyCredentials;
        ServiceSettings serviceSettings = new ServiceSettings(settings);
        Service service = new Service(serviceSettings);
        when:
        service.execService(method);
        then:
        notThrown(Exception)
    }
}