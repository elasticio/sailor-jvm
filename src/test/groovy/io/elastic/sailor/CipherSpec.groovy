package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.api.Message
import spock.lang.Shared
import spock.lang.Specification

import javax.json.Json

class CipherSpec extends Specification {

    @Shared
    def cipher;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        cipher = injector.getInstance(CipherWrapper.class);
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
        def result = cipher.encryptMessageContent(content)
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
        result == "TOxRVfC2S4QDUzw6tzpoVNzi5ldNj+qGGrx2bMJLTn+0mgv3+xZNxMPHI5HdsTq+pBF3oXzgNmaFkXWGou0rPkyhSdpk/ZjI6YciJrFhtOk9Bgh5ScAO/cZYChDertRLGjGNtm4/XTVdYCw5LBdyYDSoGfYt2K+09NtzoOGrK4KGAKhZm4BaEfCFTeGUvXpSCaiUxaHxro7OpxvO1Y5EA/ZBJIXWjhTMyc8E0WF12+wCq1eByfl5WXvEOqksfk1FGOIjqxCn9UEo995Y2f0YMA==";
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

        return new Message(body, attachments);
    }

    def "should encrypt message"() {
        when:
        def result = cipher.encryptMessage(getMessage());
        then:
        result == "TOxRVfC2S4QDUzw6tzpoVNzi5ldNj+qGGrx2bMJLTn+0mgv3+xZNxMPHI5HdsTq+pBF3oXzgNmaFkXWGou0rPkyhSdpk/ZjI6YciJrFhtOk9Bgh5ScAO/cZYChDertRLGjGNtm4/XTVdYCw5LBdyYDSoGfYt2K+09NtzoOGrK4KGAKhZm4BaEfCFTeGUvXpSCaiUxaHxro7OpxvO1Y5EA/ZBJIXWjhTMyc8E0WF12+wCq1eByfl5WXvEOqksfk1FGOIjqxCn9UEo995Y2f0YMA=="
    }

    def "should decrypt message"() {
        given:
        def encoded = "TOxRVfC2S4QDUzw6tzpoVNzi5ldNj+qGGrx2bMJLTn+0mgv3+xZNxMPHI5HdsTq+pBF3oXzgNmaFkXWGou0rPkyhSdpk/" +
                "ZjI6YciJrFhtOk9Bgh5ScAO/cZYChDertRLGjGNtm4/XTVdYCw5LBdyYDSoGfYt2K+09NtzoOGrK4KGAKhZm4BaEfCFTeGU" +
                "vXpSCaiUxaHxro7OpxvO1Y5EA/ZBJIXWjhTMyc8E0WF12+wCq1eByfl5WXvEOqksfk1FGOIjqxCn9UEo995Y2f0YMA=="
        when:
        def result = cipher.decryptMessage(encoded);
        then:
        result.toString().equals(getMessage().toString())
    }

    def "should not fail in case of null message"() {
        when:
        cipher.decryptMessage(null);
        then:
        notThrown(RuntimeException)
    }
}
