package io.elastic.sailor.impl

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.apache.http.auth.UsernamePasswordCredentials
import com.github.tomakehurst.wiremock.WireMockServer
import static com.github.tomakehurst.wiremock.client.WireMock.*
import org.junit.Rule
import spock.lang.Specification

import javax.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static org.hamcrest.Matchers.equalToIgnoringCase

class HttpUtilsSpec extends Specification {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule(12345);

    def "should post json successfully when credentials are inside url"() {

        setup:
        def body = Json.createObjectBuilder()
                .add('foo', 'barbaz')
                .build()

        driver.addExpectation(
                onRequestTo("/v1/exec/result/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("homer.simpson@example.org", "secret")
                        .withBody(equalToIgnoringCase('{"foo":"barbaz"}'), "application/json"),
                giveResponse('{"status":"done"}', 'application/json')
                        .withStatus(200));

        when:
        def result = HttpUtils.postJson(
                "http://homer.simpson%40example.org:secret@localhost:12345/v1/exec/result/55e5eeb460a8e2070000001e",
                body)

        then:

        result == '{"status":"done"}'
    }

    def "should post json successfully"() {

        setup:
        def body = Json.createObjectBuilder()
                .add('foo', 'barbaz')
                .build()

        driver.addExpectation(
                onRequestTo("/v1/exec/result/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("homer.simpson@example.org", "secret")
                        .withBody(equalToIgnoringCase('{"foo":"barbaz"}'), "application/json"),
                giveResponse('{"status":"done"}', 'application/json')
                        .withStatus(200));

        when:
        def result = HttpUtils.postJson(
                "http://localhost:12345/v1/exec/result/55e5eeb460a8e2070000001e",
                body,
                new UsernamePasswordCredentials("homer.simpson@example.org", "secret"),
                0)

        then:

        result == '{"status":"done"}'
    }

    def "should put json successfully"() {

        setup:
        def body = Json.createObjectBuilder()
                .add('foo', 'barbaz')
                .build()

        driver.addExpectation(
                onRequestTo("/v1/accounts/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.PUT)
                        .withBasicAuth("homer.simpson@example.org", "secret")
                        .withBody(equalToIgnoringCase('{"foo":"barbaz"}'), "application/json"),
                giveResponse('{"id":"55e5eeb460a8e2070000001e"}', 'application/json')
                        .withStatus(200));

        when:
        def result = HttpUtils.putJson(
                "http://localhost:12345/v1/accounts/55e5eeb460a8e2070000001e",
                body,
                new UsernamePasswordCredentials("homer.simpson@example.org", "secret"))

        then:

        result.toString() == '{"id":"55e5eeb460a8e2070000001e"}'
    }

    def "should get json successfully"() {

        setup:
        driver.addExpectation(
                onRequestTo("/v1/users")
                        .withBasicAuth("admin", "secret"),
                giveResponse('{"id":"1","email":"homer.simpson@example.org"}', 'application/json')
                        .withStatus(200));

        when:
        def result = HttpUtils.getJson(
                "http://localhost:12345/v1/users",
                new UsernamePasswordCredentials("admin", "secret"))

        then:

        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'
    }

    def "should retry getting json"() {

        def wireMockServer = new WireMockServer(12346);

        setup:
        wireMockServer.start()

        configureFor("localhost", 12346)

        stubFor(get(urlEqualTo("/econnreset")).inScenario("retry")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
                .willSetStateTo("next request"))

        stubFor(get(urlEqualTo("/econnreset")).inScenario("retry")
                .whenScenarioStateIs("next request")
                .willReturn(
                    aResponse().withStatus(200).withBody('{"id":"1","email":"homer.simpson@example.org"}')
        ))

        when:
        def result = HttpUtils.getJson(
                "http://localhost:12346/econnreset",
                new UsernamePasswordCredentials("admin", "secret"), 2)

        then:
        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'

        cleanup:
        wireMockServer.stop()
    }

    def "should send delete successfully"() {

        setup:
        driver.addExpectation(
                onRequestTo("/v1/users/1234567")
                        .withMethod(ClientDriverRequest.Method.DELETE)
                        .withBasicAuth("admin", "secret"),
                giveResponse('{"message":"ok"}', 'application/json')
                        .withStatus(200));

        when:
        def result = HttpUtils.delete(
                "http://localhost:12345/v1/users/1234567",
                new UsernamePasswordCredentials("admin", "secret"),
                0)

        then:

        result.toString() == '{"message":"ok"}'
    }

    def "should fail to post json if user info not present in the url"() {

        when:
        HttpUtils.postJson(
                "http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e",
                Json.createObjectBuilder().build())
        then:
        def e = thrown(RuntimeException)
        e.message.contains 'User info is missing in the given url: http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e'
    }
}
