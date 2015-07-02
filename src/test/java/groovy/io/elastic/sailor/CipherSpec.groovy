package groovy.io.elastic.sailor

import io.elastic.sailor.CipherWrapper
import spock.lang.Specification

class CipherSpec extends Specification {
    def "should encrypt & decrypt strings"() {
        given:
            def content = "Hello world";
            def cipher = new CipherWrapper()
        when:
            def result = cipher.encryptMessageContent(content);
            def decryptedResult = cipher.decryptMessageContent(result);
        then:
            decryptedResult.toString() == content.toString();
    }
}
