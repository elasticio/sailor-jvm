package io.elastic.sailor

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import com.google.gson.JsonObject
import org.apache.http.auth.UsernamePasswordCredentials
import org.junit.Rule
import spock.lang.Specification

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static org.hamcrest.Matchers.equalToIgnoringCase

class UtilsSpec extends Specification {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule(12345);

    def "should detect json object in a string"() {
        when:
        def result = Utils.isJsonObject("{\"body\":\"test\"}")
        then:
        notThrown(RuntimeException)
        result == true
    }

    def "should detect json object property"() {

        JsonObject body = new JsonObject();
        body.addProperty("somekey", "somevalue");

        JsonObject message = new JsonObject();
        message.add("body", body);

        when:
        def result = Utils.isJsonObject(message.get("body"))
        then:
        notThrown(RuntimeException)
        result == true
    }

    def "should process NULL well"() {

        when:
        def result = Utils.isJsonObject(null)
        then:
        notThrown(RuntimeException)
        result == false
    }

    def "should post json successfully"() {

        setup:
        def body = new JsonObject()
        body.addProperty('foo', 'barbaz')

        driver.addExpectation(
                onRequestTo("/v1/exec/result/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBasicAuth("homer.simpson@example.org", "secret")
                        .withBody(equalToIgnoringCase('{"foo":"barbaz"}'), "application/json"),
                giveResponse('{"status":"done"}', 'application/json')
                        .withStatus(200));

        when:
        def result =Utils.postJson(
                "http://homer.simpson%40example.org:secret@localhost:12345/v1/exec/result/55e5eeb460a8e2070000001e",
                body)

        then:

        result == '{"status":"done"}'
    }

    def "should get json successfully"() {

        setup:
        driver.addExpectation(
                onRequestTo("/v1/users")
                        .withBasicAuth("admin", "secret"),
                giveResponse('{"id":"1","email":"homer.simpson@example.org"}', 'application/json')
                        .withStatus(200));

        when:
        def result = Utils.getJson(
                "http://admin:secret@localhost:12345/v1/users",
                new UsernamePasswordCredentials("admin", "secret"))

        then:

        result.toString() == '{"id":"1","email":"homer.simpson@example.org"}'
    }

    def "should fail to post json if user info not present in the url"() {

        when:
        Utils.postJson(
                "http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e",
                new JsonObject())
        then:
        def e = thrown(RuntimeException)
        e.message.contains 'User info is missing in the given url: http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e'
    }
}
