package io.elastic.sailor

import com.google.gson.JsonObject
import spock.lang.Specification

class UtilsSpec extends SetupServerHelper {

    def getHandler() {
        new SimpleRequestHandler()
    }

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

        when:
        Utils.postJson(
                "http://homer%2Bsimpson%40example.org:secret@localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e",
                body)
        then:

        System.err.println SimpleRequestHandler.headers
        System.err.println SimpleRequestHandler.headers.keySet()
        SimpleRequestHandler.lastMessage == '{"foo":"barbaz"}'
        SimpleRequestHandler.headers.containsKey("Content-Type")
        SimpleRequestHandler.headers.get("Content-Type") == 'application/json'
        SimpleRequestHandler.headers.get("Authorization") == 'Basic aG9tZXIrc2ltcHNvbkBleGFtcGxlLm9yZzpzZWNyZXQ='
    }

    def "should fail to post json if user info not present in the url"() {

        when:
        Utils.postJson(
                "http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e",
                new JsonObject())
        then:
        def e = thrown(RuntimeException)
        e.message == 'User info is missing in the given url: http://localhost:10000/v1/exec/result/55e5eeb460a8e2070000001e'
    }
}
