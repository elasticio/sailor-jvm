package groovy.io.elastic.sailor
import com.google.gson.JsonObject
import io.elastic.sailor.CipherWrapper
import spock.lang.Specification
import io.elastic.api.Message

class CipherSpec extends Specification {

    def key = "testCryptoPassword"

    def "should encrypt & decrypt strings"() {
        given:
            def content = "Hello world"
            def cipher = new CipherWrapper(key)
        when:
            def result = cipher.encrypt(content)
            def decryptedResult = cipher.decrypt(result)
        then:
            decryptedResult.toString() == content.toString()
    }

    def "should encrypt & decrypt objects"() {
        given:
            def content = new JsonObject()
            content.addProperty("property1", "Hello world")
            def cipher = new CipherWrapper(key)
        when:
            def result = cipher.encrypt(content)
            def decryptedResult = cipher.decrypt(result)
        then:
            decryptedResult.toString() == content.toString()
    }

    def "should throw error if failed to decrypt"() {
        given:
            def cipher = new CipherWrapper(key)
        when:
            cipher.decrypt("dsdasdsad");
        then: // TODO: should throw RuntimeException if input string is not JsonElement
            notThrown(RuntimeException)
    }

    def "should decrypt messages encrypted in Node.js"() {
        given:
            def cipher = new CipherWrapper(key)
        when:
            def result = cipher.decryptMessageContent("MhcbHNshDRy6RNubmFJ+u4tcKKTKT6H50uYMyBXhws1xjvVKRtEC0hEg0/R2Zecy");
        then:
            result.get("someKey").getAsString() == "someValue"
    }

    def "should encrypt messages so that Node.js understands"() {
        given:
            def cipher = new CipherWrapper(key)
            def body = new JsonObject()
            body.addProperty("someKey", "someValue")
        when:
            def result = cipher.encryptMessageContent(body);
        then:
            result == "MhcbHNshDRy6RNubmFJ+u4tcKKTKT6H50uYMyBXhws1xjvVKRtEC0hEg0/R2Zecy"
    }
}
