package io.elastic.sailor

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo

abstract class ApiAwareSpecification extends Specification {

    @ClassRule
    @Shared
    public ClientDriverRule driver = new ClientDriverRule(11111);

    def setupSpec() {

        driver.addExpectation(
                onRequestTo("/")
                        .withMethod(ClientDriverRequest.Method.GET)
                        .withBasicAuth("admin", "secret"),
                giveResponse(
                        "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}",
                        'application/json')
                        .withStatus(200));
    }
}
