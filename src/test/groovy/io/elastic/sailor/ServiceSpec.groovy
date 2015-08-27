package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector

class ServiceSpec extends SetupServerHelper {

    def getHandler() {
        new SimpleRequestHandler()
    }


    def service;

    def setup() {
        Injector injector = Guice.createInjector(new SailorModule(), new TestModule())

        service = injector.getInstance(Service.class)
    }

    def cleanup() {
        SimpleRequestHandler.lastMessage = ""
    }

    def "it should verify credentials"() {
        when:
        service.start(ServiceMethods.verifyCredentials)

        then:
        SimpleRequestHandler.lastMessage == '{"verified":true}'
    }

    def "it should get meta model"() {
        when:
        service.start(ServiceMethods.getMetaModel);

        then:
        SimpleRequestHandler.lastMessage == '{}'
    }

    def "it should get select model"() {
        when:
        service.start(ServiceMethods.selectModel)

        then:
        SimpleRequestHandler.lastMessage == '{}'
    }

    def "it throw IllegalArgumentException if too few arguments"() {
        setup:

        def args = [""] as String[]

        when:
        Service.main(args);

        then:
        def e = thrown(IllegalArgumentException)
        e.message == '3 arguments are required, but were passed 1'
    }
}