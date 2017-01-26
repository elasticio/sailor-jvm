package io.elastic.sailor.impl

import io.elastic.api.JSON
import io.elastic.sailor.ApiClient
import io.elastic.sailor.Step
import spock.lang.Specification

import javax.json.Json

class UpdateKeysCallbackSpec extends Specification {

    def apiClient = Mock(ApiClient)

    def step = JSON.parseObject("{" +
            "\"id\":\"step_1\"," +
            "\"comp_id\":\"testcomponent\"," +
            "\"function\":\"test\"," +
            "\"config\":{\"_account\":\"5559edd38968ec0736000003\"}," +
            "\"snapshot\":{\"timestamp\":\"19700101\"}}")

    def callback = new UpdateKeysCallback(new Step(step), apiClient)

    def "should updateAccount successfully"() {

        setup:
        def keys = Json.createObjectBuilder()
                .add('apiSecret', 'barbaz')
                .build()

        when:
        callback.receive(keys)

        then:
        1 * apiClient.updateAccount("5559edd38968ec0736000003",{
            it.toString() == '{"keys":{"apiSecret":"barbaz"}}'
        })
    }
}
