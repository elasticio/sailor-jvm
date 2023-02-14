package io.elastic.sailor.impl

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.SailorModule
import io.elastic.sailor.SailorTestModule
import spock.lang.Shared
import spock.lang.Specification

import jakarta.json.Json

class CryptoServiceImplSpec extends Specification {

    @Shared
    def cipher;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        cipher = injector.getInstance(CryptoServiceImpl.class);
    }

    def "should encrypt & decrypt strings - base64"() {
        when:
        def result = cipher.encrypt("Hello world!", MessageEncoding.BASE64)
        def decryptedResult = cipher.decrypt(result, MessageEncoding.BASE64)
        then:
        decryptedResult.toString() == "Hello world!"
    }

    def "should encrypt & decrypt strings - utf8"() {
        when:
        def result = cipher.encrypt("Hello world!", MessageEncoding.UTF8)
        def decryptedResult = cipher.decrypt(result, MessageEncoding.UTF8)
        then:
        decryptedResult.toString() == "Hello world!"
    }

    def "should encrypt & decrypt objects - base64"() {
        given:
        def content = Json.createObjectBuilder()
                .add("property1", "Hello world!")
                .build()
        when:
        def result = cipher.encryptJsonObject(content, MessageEncoding.BASE64)
        def decryptedResult = cipher.decryptMessageContent(result, MessageEncoding.BASE64)
        then:
        decryptedResult.toString() == '{"property1":"Hello world!"}'
    }

    def "should encrypt & decrypt objects - utf8"() {
        given:
        def content = Json.createObjectBuilder()
                .add("property1", "Hello world!")
                .build()
        when:
        def result = cipher.encryptJsonObject(content, MessageEncoding.UTF8)
        def decryptedResult = cipher.decryptMessageContent(result, MessageEncoding.UTF8)
        then:
        decryptedResult.toString() == '{"property1":"Hello world!"}'
    }

    def "should throw error if failed to decrypt"() {
        when:
        cipher.decrypt("dsdasdsad", MessageEncoding.BASE64);
        then: // TODO: should throw RuntimeException if input string is not JsonElement
        thrown(RuntimeException)
    }

    def "should decrypt JSON objects encrypted in Node.js"() {
        when:
        def result = cipher.decryptMessageContent(
                "vSx5ntK2UdYh2Wjcdy8rgM7Yz5a/H8koXKtwNI0FL/Y9QiQFcUrtT4HJUkYXACNL".getBytes(), MessageEncoding.BASE64);
        then:
        result.get("body").toString() == "{\"someKey\":\"someValue\"}"
    }

    def "should encrypt JSON objects so that Node.js understands"() {
        when:
        def result = cipher.encrypt(getMessage().toString(), MessageEncoding.BASE64);
        then:
        new String(result) == "yM4zPVWl1cDydyIRhie5MB6imzdt9gAxsdeHxu7re4h/mUjCGbVNNOZSq/uBGe9eMt6uqDI3OZ4CnruatPT0XF0YxaWPLZxuaET1AoFUqYuNr+n/pxJ0XKkoaAtvFdH5McqftOxzMGGh8CRC+4ZYwF0PYeLT2vyzy+Lri55HNnbRc0bYLfY+UovG2uIFFMPfqV+qQgvyNXT0IFmawaV92Rb26iKqeOD9eYo9gXaYikwXaHBh6DpbdM4mJSmGEf0pIHwAyIajTGuPfGjo9jm1SMfIivX2gb0YanmQlP0a/VF9IdBYe/PKwESqKdrCBdaZ1amjhcpMOzQzwVaWetHKtUebmDShJKJzumvoBL0cPpQNjYY0eCiC2RHX/5tJig5tPBv5vbetkl2duS6UZFu5kw==";
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
                .method("GET")
                .query(Json.createObjectBuilder().build())
                .build();
    }

    def "should encrypt message"() {
        when:
        def result = cipher.encryptMessage(getMessage(), MessageEncoding.BASE64);
        then:
        new String(result) == "yM4zPVWl1cDydyIRhie5MB6imzdt9gAxsdeHxu7re4h/mUjCGbVNNOZSq/uBGe9eMt6uqDI3OZ4CnruatPT0XF0YxaWPLZxuaET1AoFUqYuNr+n/pxJ0XKkoaAtvFdH5McqftOxzMGGh8CRC+4ZYwF0PYeLT2vyzy+Lri55HNnbRc0bYLfY+UovG2uIFFMPfqV+qQgvyNXT0IFmawaV92Rb26iKqeOD9eYo9gXaYikwXaHBh6DpbdM4mJSmGEf0pIHwAyIajTGuPfGjo9jm1SMfIivX2gb0YanmQlP0a/VF9IdBYe/PKwESqKdrCBdaZ1amjhcpMOzQzwVaWetHKtUebmDShJKJzumvoBL0cPpQNjYY0eCiC2RHX/5tJig5tPBv5vbetkl2duS6UZFu5kw=="
    }

    def "should not fail in case of null message"() {
        when:
        def result = cipher.decryptMessageContent(null, MessageEncoding.BASE64);
        then:
        JSON.stringify(result) == '{}'
    }
}
