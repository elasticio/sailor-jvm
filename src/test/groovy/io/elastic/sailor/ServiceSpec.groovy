package io.elastic.sailor

class ServiceSpec extends SetupServerHelper {

    def getHandler() {
        new SimpleRequestHandler()
    }

    def cleanup() {
        SimpleRequestHandler.lastMessage = ""
    }

    def "it should verify credentials"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_POST_RESULT_URL, "http://localhost:10000")
        System.setProperty(ServiceSettings.ENV_VAR_ACTION_OR_TRIGGER, "test")
        System.setProperty(ServiceSettings.ENV_VAR_CFG, "{\"key\":0}")

        ServiceMethods method = ServiceMethods.verifyCredentials;
        System.setProperty(ServiceSettings.ENV_VAR_GET_MODEL_METHOD, method.name())

        def args = ["", "", method.name()] as String[]

        when:
        Service.main(args);

        then:
        SimpleRequestHandler.lastMessage == '{"verified":true}'
    }

    def "it should get meta model"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_POST_RESULT_URL, "http://localhost:10000")
        System.setProperty(ServiceSettings.ENV_VAR_ACTION_OR_TRIGGER, "test")
        System.setProperty(ServiceSettings.ENV_VAR_CFG, "{\"key\":0}")
        ServiceMethods method = ServiceMethods.getMetaModel;

        System.setProperty(ServiceSettings.ENV_VAR_GET_MODEL_METHOD, method.name())

        def args = ["", "", method.name()] as String[]

        when:
        Service.main(args);

        then:
        SimpleRequestHandler.lastMessage == '{}'
    }

    def "it should get select model"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_POST_RESULT_URL, "http://localhost:10000")
        System.setProperty(ServiceSettings.ENV_VAR_ACTION_OR_TRIGGER, "test")
        System.setProperty(ServiceSettings.ENV_VAR_CFG, "{\"key\":0}")
        ServiceMethods method = ServiceMethods.selectModel;

        System.setProperty(ServiceSettings.ENV_VAR_GET_MODEL_METHOD, method.name())

        def args = ["", "", method.name()] as String[]

        when:
        Service.main(args);

        then:
        SimpleRequestHandler.lastMessage == '{}'
    }

    def "it throw IllegalArgumentException if too few arguments"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_POST_RESULT_URL, "http://localhost:10000")
        System.setProperty(ServiceSettings.ENV_VAR_ACTION_OR_TRIGGER, "test")
        System.setProperty(ServiceSettings.ENV_VAR_CFG, "{\"key\":0}")
        ServiceMethods method = ServiceMethods.selectModel;

        System.setProperty(ServiceSettings.ENV_VAR_GET_MODEL_METHOD, method.name())

        def args = [""] as String[]

        when:
        Service.main(args);

        then:
        def e = thrown(IllegalArgumentException)
        e.message == '3 arguments are required, but were passed 1'
    }
}