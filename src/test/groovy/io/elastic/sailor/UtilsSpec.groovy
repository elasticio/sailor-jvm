package io.elastic.sailor

import spock.lang.Specification

import jakarta.json.Json
import jakarta.json.JsonObject

class UtilsSpec extends Specification {

    def "should detect json object in a string"() {
        when:
        def result = Utils.isJsonObject("{\"body\":\"test\"}")
        then:
        notThrown(RuntimeException)
        result == true
    }

    def "should detect json object property"() {

        JsonObject body = Json.createObjectBuilder()
                .add("somekey", "somevalue")
                .build()

        JsonObject message = Json.createObjectBuilder()
                .add("body", body)
                .build()

        when:
        def result = Utils.isJsonObject(message.get("body").toString())
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
}
