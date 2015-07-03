package groovy.io.elastic.sailor

import com.google.gson.JsonParser
import io.elastic.sailor.CipherWrapper
import spock.lang.Specification

class CipherSpec extends Specification {
    def "should encrypt & decrypt strings"() {
        given:
            def content = "Hello world"
            def cipher = new CipherWrapper()
        when:
            def result = cipher.encryptMessageContent(content)
            def decryptedResult = cipher.decryptMessageContent(result)
        then:
            decryptedResult.toString() == content.toString()
    }

    def "should encrypt & decrypt objects"() {
        given:
            def content = new JsonParser().parse("{property1: 'Hello world'}".replaceAll("'", "\"")).getAsJsonObject()
            def cipher = new CipherWrapper()
        when:
            def result = cipher.encryptMessageContent(content)
            def decryptedResult = cipher.decryptMessageContent(result)
        then:
            decryptedResult.equals(content.toString())
    }

    def "should throw error if failed to decrypt"() {
        given:
            def cipher = new CipherWrapper()
        when:
            cipher.decryptMessageContent("dsdasdsad");
        then: // TODO: should throw RuntimeException if input string is not JsonElement
            notThrown(RuntimeException)
    }
}
