package io.elastic.sailor

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import org.junit.Rule
import spock.lang.Specification

import javax.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static org.hamcrest.Matchers.equalToIgnoringCase

class ApiClientImplSpec extends Specification {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule(7890);

    def client = new ApiClientImpl("http://localhost:7890", "homer.simpson@example.org", "secret")
    def stepJson = '{"id":"step_1", "comp_id":"comp_1", "function":"my_function", "cfg":{}, "snapshot":{}}'

    def "should retrieveTaskStep successfully"() {

        setup:

        driver.addExpectation(
                onRequestTo("/v1/tasks/55e5eeb460a8e2070000001e/steps/step_1")
                        .withMethod(ClientDriverRequest.Method.GET)
                        .withBasicAuth("homer.simpson@example.org", "secret"),
                giveResponse(stepJson, 'application/json')
                        .withStatus(200));

        when:
        def step = client.retrieveTaskStep("55e5eeb460a8e2070000001e", "step_1");

        then:

        step.id == 'step_1'
        step.compId == "comp_1"
        step.function == "my_function"
        step.cfg.toString() == "{}"
        step.snapshot.toString() == "{}"
    }

    def "should updateAccount successfully"() {

        setup:
        def keys = Json.createObjectBuilder()
                .add('apiSecret', 'barbaz')
                .build()

        def body = Json.createObjectBuilder()
                .add('keys', keys)
                .build()

        driver.addExpectation(
                onRequestTo("/v1/accounts/55083c567aea6f030000001a")
                        .withMethod(ClientDriverRequest.Method.PUT)
                        .withBasicAuth("homer.simpson@example.org", "secret")
                        .withBody(equalToIgnoringCase('{"keys":{"apiSecret":"barbaz"}}'), "application/json"),
                giveResponse('{"id": "55083c567aea6f030000001a", "keys":{"apiSecret":"barbaz"}}', 'application/json')
                        .withStatus(200));

        when:
        def result = client.updateAccount("55083c567aea6f030000001a", body)

        then:

        result.toString() == '{"id":"55083c567aea6f030000001a","keys":{"apiSecret":"barbaz"}}'
    }
}
