package io.elastic.sailor.impl

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.SailorModule
import io.elastic.sailor.SailorTestModule
import spock.lang.Shared
import spock.lang.Specification

import javax.json.Json

class CryptoServiceImplSpec extends Specification {

    @Shared
    def cipher;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        cipher = injector.getInstance(CryptoServiceImpl.class);
    }

    def "should encrypt & decrypt strings"() {
        when:
        def result = cipher.encrypt("Hello world!")
        def decryptedResult = cipher.decrypt(result)
        then:
        decryptedResult.toString() == "Hello world!"
    }

    def "should encrypt & decrypt objects"() {
        given:
        def content = Json.createObjectBuilder()
                .add("property1", "Hello world!")
                .build()
        when:
        def result = cipher.encryptJsonObject(content)
        def decryptedResult = cipher.decryptMessageContent(result)
        then:
        decryptedResult.toString() == '{"property1":"Hello world!"}'
    }

    def "should throw error if failed to decrypt"() {
        when:
        cipher.decrypt("dsdasdsad");
        then: // TODO: should throw RuntimeException if input string is not JsonElement
        thrown(RuntimeException)
    }

    def "should decrypt JSON objects encrypted in Node.js"() {
        when:
        def result = cipher.decryptMessageContent(
                "vSx5ntK2UdYh2Wjcdy8rgM7Yz5a/H8koXKtwNI0FL/Y9QiQFcUrtT4HJUkYXACNL");
        then:
        result.get("body").toString() == "{\"someKey\":\"someValue\"}"
    }

    def "should encrypt JSON objects so that Node.js understands"() {
        when:
        def result = cipher.encrypt(getMessage().toString());
        then:
        result == "yM4zPVWl1cDydyIRhie5MB6imzdt9gAxsdeHxu7re4i8MmV3oYGOQ5oRiAWWHeZHPNgCR7v9Dn0GV1pr5wAsIudUSSPldoRmggRlqg+VyTKrsxmdSyM7h6vqyRqPRNJic/ZJwJL3GxU/EEW5rHXrcJdBxQa7GZOV1MuFxC0vMh9cRJxErtWoojI3hWnEwtr0Qhj70/9JQ7l2ueejDfDeKoHM5z2OFY5dzoyQPEAkUVaOm/lrp97DOAY85xiuIkbl/RGvDy98boDd0QZloWNdwXlmDEGzVsFnBGMM29T5USM9n1jD+kmpT2qgLXNNe0YRCHG7Tz1InYdg4h7UA2D8xMjvPYjllg7qvV3DOazKCQfoWARuzEZUOIl2Ev0814wR";
    }

    def getMessage() {
        def body = Json.createObjectBuilder()
                .add("incomingProperty1", "incomingValue1")
                .add("incomingProperty2", "incomingValue2")
                .build()

        def attachments = Json.createObjectBuilder()
                .add("incomingAttachment1", "incomingAttachment1Content")
                .add("incomingAttachment2", "incomingAttachment2Content")
                .build()

        new Message.Builder()
                .id(UUID.fromString("88999c40-a2f4-404e-9bf3-c531a37c9df4"))
                .headers(Json.createObjectBuilder().build())
                .body(body)
                .attachments(attachments)
                .build();
    }

    def "should encrypt message"() {
        when:
        def result = cipher.encryptMessage(getMessage());
        then:
        result == "yM4zPVWl1cDydyIRhie5MB6imzdt9gAxsdeHxu7re4i8MmV3oYGOQ5oRiAWWHeZHPNgCR7v9Dn0GV1pr5wAsIudUSSPldoRmggRlqg+VyTKrsxmdSyM7h6vqyRqPRNJic/ZJwJL3GxU/EEW5rHXrcJdBxQa7GZOV1MuFxC0vMh9cRJxErtWoojI3hWnEwtr0Qhj70/9JQ7l2ueejDfDeKoHM5z2OFY5dzoyQPEAkUVaOm/lrp97DOAY85xiuIkbl/RGvDy98boDd0QZloWNdwXlmDEGzVsFnBGMM29T5USM9n1jD+kmpT2qgLXNNe0YRCHG7Tz1InYdg4h7UA2D8xMjvPYjllg7qvV3DOazKCQfoWARuzEZUOIl2Ev0814wR"
    }

    def "should not fail in case of null message"() {
        when:
        def result = cipher.decryptMessageContent(null);
        then:
        JSON.stringify(result) == '{}'
    }
}
