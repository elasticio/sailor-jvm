package io.elastic.sailor

import com.google.gson.Gson
import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.api.JSON
import io.elastic.sailor.component.SimpleSelectModelProvider

class ServiceSpec extends SetupServerHelper {

    def getHandler() {
        new SimpleRequestHandler()
    }


    def injector
    def service;

    def setup() {
        injector = Guice.createInjector(new ServiceModule(), new TestServiceEnvironmentModule())

        service = injector.getInstance(Service.class)
    }

    def cleanup() {
        SimpleSelectModelProvider.SHOULD_FAIL = false
    }

    def "it should verify credentials"() {
        when:
        service.executeMethod(ServiceMethods.verifyCredentials)

        then:
        SimpleRequestHandler.lastMessage == '{"status":"success","data":{"verified":true}}'
    }

    def "it should get meta model"() {
        when:
        service.executeMethod(ServiceMethods.getMetaModel);

        then:
        SimpleRequestHandler.lastMessage == '{"status":"success","data":{"in":{"type":"object"},"out":{}}}'
    }

    def "it should get select model"() {
        when:
        service.executeMethod(ServiceMethods.selectModel)

        then:
        SimpleRequestHandler.lastMessage == '{"status":"success","data":{"de":"Germany","us":"United States","cfg":{"key":0}}}'
    }

    def "it should get select model s"() {
        setup:
        SimpleSelectModelProvider.SHOULD_FAIL = true

        when:
        Service.getServiceInstanceAndExecute(ServiceMethods.selectModel, injector);

        then:
        def response = JSON.parse(SimpleRequestHandler.lastMessage)
        response.get("status").getAsString() == 'error'
        response.get("data").getAsJsonObject()
                .get('message').getAsString()
                .startsWith('java.lang.RuntimeException: Spec author told me to fail')
    }

    def "it throw IllegalArgumentException if too few arguments"() {
        setup:

        def args = [] as String[]

        when:
        Service.main(args);

        then:
        def e = thrown(IllegalArgumentException)
        e.message == '1 argument is required, but were passed 0'
    }
}