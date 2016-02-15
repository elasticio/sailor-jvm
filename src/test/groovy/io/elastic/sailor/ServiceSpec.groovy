package io.elastic.sailor

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import com.google.inject.Guice
import io.elastic.sailor.component.SimpleSelectModelProvider
import org.junit.Rule
import spock.lang.Specification

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalToIgnoringCase

class ServiceSpec extends Specification {


    @Rule
    public ClientDriverRule driver = new ClientDriverRule(10000);


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
        setup:

        driver.addExpectation(
                onRequestTo("/")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("admin", "secret")
                        .withBody(equalToIgnoringCase('{"status":"success","data":{"verified":true}}'), "application/json"),
                giveResponse('{"message":"ok"}', 'application/json')
                        .withStatus(200));
        expect:
        service.executeMethod(ServiceMethods.verifyCredentials)
    }

    def "it should get meta model"() {
        setup:

        driver.addExpectation(
                onRequestTo("/")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("admin", "secret")
                        .withBody(equalToIgnoringCase(
                        '{"status":"success","data":{"in":{"type":"object"},"out":{}}}'),
                        "application/json"),
                giveResponse('{"message":"ok"}', 'application/json')
                        .withStatus(200));


        expect:
        service.executeMethod(ServiceMethods.getMetaModel);
    }

    def "it should get select model"() {
        setup:

        driver.addExpectation(
                onRequestTo("/")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("admin", "secret")
                        .withBody(equalToIgnoringCase(
                        '{"status":"success","data":{"de":"Germany","us":"United States","cfg":{"key":0}}}'),
                        "application/json"),
                giveResponse('{"message":"ok"}', 'application/json')
                        .withStatus(200));
        expect:
        service.executeMethod(ServiceMethods.selectModel)
    }

    def "it should post failure details and rethrow exception"() {
        setup:
        SimpleSelectModelProvider.SHOULD_FAIL = true

        driver.addExpectation(
                onRequestTo("/")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("admin", "secret")
                        .withBody(containsString('{"status":"error","data":{"message":"java.lang.RuntimeException: Spec author told me to fail\\n\\tat io.elastic.sailor.component.SimpleSelectModelProvider.getSelectModel(SimpleSelectModelProvider.java:14)'),
                        "application/json"),
                giveResponse('{"message":"ok"}', 'application/json')
                        .withStatus(200));

        when:
        Service.getServiceInstanceAndExecute(ServiceMethods.selectModel, injector);

        then:

        def e = thrown(RuntimeException)
        e.message == 'java.lang.RuntimeException: Spec author told me to fail'
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