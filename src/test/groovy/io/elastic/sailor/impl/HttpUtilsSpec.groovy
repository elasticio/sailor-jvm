package io.elastic.sailor.impl

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.elastic.sailor.impl.HttpUtils.BasicAuthorizationHandler
import io.elastic.sailor.impl.HttpUtils.BasicURLAuthorizationHandler
import org.junit.Rule
import spock.lang.Specification

import jakarta.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static com.github.tomakehurst.wiremock.client.WireMock.*
import static org.hamcrest.Matchers.equalToIgnoringCase

class HttpUtilsSpec extends Specification {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule(12345);

    def basicURLAuthHandler = new HttpUtils.BasicURLAuthorizationHandler();
    def basicAuthHandler = new BasicAuthorizationHandler("homer.simpson@example.org", "secret")

    def "should post json successfully when credentials are inside url"() {

        setup:
        def body = Json.createObjectBuilder()
                .add('foo', 'barbaz')
                .build()
        def httpClient = HttpUtils.createHttpClient(3)

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
                httpClient,
                body,
                basicURLAuthHandler)

        then:
        result == '{"status":"done"}'

        cleanup:
        httpClient.close()
    }

    def "should post json successfully"() {

        setup:
        def body = Json.createObjectBuilder()
                .add('foo', 'barbaz')
                .build()
        def httpClient = HttpUtils.createHttpClient(3)

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
                httpClient,
                body,
                basicAuthHandler)

        then:
        result == '{"status":"done"}'

        cleanup:
        httpClient.close()
    }

    def "should put json successfully"() {

        setup:
        def body = Json.createObjectBuilder()
                .add('foo', 'barbaz')
                .build()
        def httpClient = HttpUtils.createHttpClient(3)

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
                httpClient,
                body,
                basicAuthHandler)

        then:
        result.toString() == '{"id":"55e5eeb460a8e2070000001e"}'

        cleanup:
        httpClient.close()
    }

    def "should get json successfully"() {

        setup:
        def httpClient = HttpUtils.createHttpClient(3)
        driver.addExpectation(
                onRequestTo("/v1/users")
                        .withBasicAuth("admin", "secret"),
                giveResponse('{"id":"1","email":"homer.simpson@example.org"}', 'application/json')
                        .withStatus(200));

        when:
        def result = HttpUtils.getJson(
                "http://localhost:12345/v1/users",
                httpClient,
                new BasicAuthorizationHandler("admin", "secret"))

        then:
        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'

        cleanup:
        httpClient.close()
    }

    def "should retry getting json"() {

        def wireMockServer = new WireMockServer(12346);

        setup:
        wireMockServer.start()
        def httpClient = HttpUtils.createHttpClient(3)

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
                httpClient,
                new BasicAuthorizationHandler("admin", "secret"))

        then:
        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'

        cleanup:
        httpClient.close()
        wireMockServer.stop()
    }

    def "should retry 408 Request Timeout response from server"() {
        def wireMockServer = new WireMockServer(12346);

        setup:
        wireMockServer.start()
        def httpClient = HttpUtils.createHttpClient(3)

        configureFor("localhost", 12346)

        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(408).withBody("Request Timeout"))
                .willSetStateTo("next request"))
        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs("next request")
                .willReturn(
                        aResponse().withStatus(200).withBody('{"id":"1","email":"homer.simpson@example.org"}')
                ))

        when:
        def result = HttpUtils.getJson(
                "http://localhost:12346/timeout",
                httpClient,
                new BasicAuthorizationHandler("admin", "secret"))
        then:
        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'

        cleanup:
        httpClient.close()
        wireMockServer.stop()
    }

    def "should retry 500 status code response from server"() {
        def wireMockServer = new WireMockServer(12346);

        setup:
        wireMockServer.start()
        def httpClient = HttpUtils.createHttpClient(3)

        configureFor("localhost", 12346)

        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withBody("Server Error"))
                .willSetStateTo("next request 1"))
        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs("next request 1")
                .willReturn(aResponse().withStatus(500).withBody("Server Error"))
                .willSetStateTo("next request 2"))
        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs("next request 2")
                .willReturn(aResponse().withStatus(500).withBody("Server Error"))
                .willSetStateTo("next request 3"))
        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs("next request 3")
                .willReturn(
                        aResponse().withStatus(200).withBody('{"id":"1","email":"homer.simpson@example.org"}')
                ))

        when:
        def result = HttpUtils.getJson(
                "http://localhost:12346/timeout",
                httpClient,
                new BasicAuthorizationHandler("admin", "secret"))
        then:
        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'
        cleanup:
        httpClient.close()
        wireMockServer.stop()
    }

    def "should fail if retry limit is reached"() {
        def wireMockServer = new WireMockServer(12346);

        setup:
        wireMockServer.start()
        def httpClient1 = HttpUtils.createHttpClient(1);

        configureFor("localhost", 12346)

        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs(Scenario.STARTED)
                .willReturn(aResponse().withStatus(500).withBody("Server Error"))
                .willSetStateTo("next request 1"))
        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs("next request 1")
                .willReturn(aResponse().withStatus(500).withBody("Server Error"))
                .willSetStateTo("next request 2"))
        stubFor(get(urlEqualTo("/timeout")).inScenario("timeout")
                .whenScenarioStateIs("next request 2")
                .willReturn(
                        aResponse().withStatus(200).withBody('{"id":"1","email":"homer.simpson@example.org"}')
                ))

        when:
        HttpUtils.getJson(
                "http://localhost:12346/timeout",
                httpClient1,
                new BasicAuthorizationHandler("admin", "secret"))
        then:
        def e = thrown(RuntimeException)
        e.message.contains("Got 500 response")
        cleanup:
        httpClient1.close()
        wireMockServer.stop()
    }

    def "should send delete successfully"() {

        setup:
        def httpClient = HttpUtils.createHttpClient(3)
        driver.addExpectation(
                onRequestTo("/v1/users/1234567")
                        .withMethod(ClientDriverRequest.Method.DELETE)
                        .withBasicAuth("admin", "secret"),
                giveResponse('{"message":"ok"}', 'application/json')
                        .withStatus(200));

        expect:
        HttpUtils.delete(
                "http://localhost:12345/v1/users/1234567",
                httpClient,
                new BasicAuthorizationHandler("admin", "secret"))

        cleanup:
        httpClient.close()
    }

    def "should fail to post json if user info not present in the url"() {
        setup:
        def httpClient = HttpUtils.createHttpClient(3)

        when:
        HttpUtils.postJson(
                "http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e",
                httpClient,
                Json.createObjectBuilder().build(),
                new BasicURLAuthorizationHandler())
        then:
        def e = thrown(RuntimeException)
        e.message.contains 'User info is missing in the given url: http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e'

        cleanup:
        httpClient.close()
    }

    def "should not send Expect-Continue header"() {
        def wireMockServer = new WireMockServer(12346);

        setup:
        wireMockServer.start()
        def httpClient = HttpUtils.createHttpClient(0)
        def body = Json.createObjectBuilder().add("foo", "bar").build()

        configureFor("localhost", 12346)

        stubFor(post(urlEqualTo("/expect-continue-test"))
                .willReturn(aResponse().withStatus(200)))

        when:
        HttpUtils.postJson(
                "http://localhost:12346/expect-continue-test",
                httpClient,
                body,
                basicAuthHandler)

        then:
        verify(postRequestedFor(urlEqualTo("/expect-continue-test"))
                .withoutHeader("Expect"))
        
        cleanup:
        httpClient.close()
        wireMockServer.stop()
    }
}
