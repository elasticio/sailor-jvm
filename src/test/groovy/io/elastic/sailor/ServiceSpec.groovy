package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector

class ServiceSpec extends SetupServerHelper {

    def getHandler() {
        new SimpleRequestHandler()
    }


    def service;

    def setup() {
        Injector injector = Guice.createInjector(new ServiceModule(), new TestServiceEnvironmentModule())

        service = injector.getInstance(Service.class)
    }

    def "it should verify credentials"() {
        when:
        service.start(ServiceMethods.verifyCredentials)

        then:
        SimpleRequestHandler.lastMessage == '{"status":"success","data":{"verified":true}}'
    }

    def "it should get meta model"() {
        when:
        service.start(ServiceMethods.getMetaModel);

        then:
        SimpleRequestHandler.lastMessage == '{"status":"success","data":{"in":{"type":"object"},"out":{}}}'
    }

    def "it should get select model"() {
        when:
        service.start(ServiceMethods.selectModel)

        then:
        SimpleRequestHandler.lastMessage == '{"status":"success","data":{"de":"Germany","us":"United States","cfg":{"key":0}}}'
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

    // @TODO provide env var POST_RESULT_URI
    // and test that exception data is sent there
    def "it should post exception back to API"() {
        setup:

        def args = [] as String[]

        when:
        Service.main(args);

        then:
        SimpleRequestHandler.lastMessage == '{"status":"error","data":{"message":"Env var \'CFG\' is required"}}'
    }
}