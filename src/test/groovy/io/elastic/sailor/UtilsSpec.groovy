package io.elastic.sailor

import com.google.gson.JsonObject
import spock.lang.Specification

class UtilsSpec extends Specification {

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
}
