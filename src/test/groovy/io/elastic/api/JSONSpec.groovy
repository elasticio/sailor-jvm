package io.elastic.api

import spock.lang.Specification
import spock.lang.Unroll

import jakarta.json.Json

@Unroll
class JSONSpec extends Specification {

    def "parse JSON object from #input results in #result"() {
        setup:

        expect:
        JSON.parseObject(input) == result

        where:
        input << [null, "{}"]
        result << [null, Json.createObjectBuilder().build()]
    }

    def "parse JSON array from #input results in #result"() {
        expect:
        JSON.parseArray(input) == result

        where:
        input << [null, "[]"]
        result << [null, Json.createArrayBuilder().build()]
    }

    def "parsing a JSON array as JSON fails"() {
        when:
        JSON.parseObject("[]")

        then:
        def e = thrown(jakarta.json.JsonException)
        e.message == "JsonParser#getObject() or JsonParser#getObjectStream() is valid only for START_OBJECT parser state. But current parser state is START_ARRAY"
    }

    def "stringify"() {
        setup:
        def json = Json.createObjectBuilder()
                .add('hello', 'world')
                .build();
        expect:
        JSON.stringify(json) == '{"hello":"world"}'
    }
}