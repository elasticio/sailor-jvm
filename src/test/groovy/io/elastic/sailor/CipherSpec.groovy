package io.elastic.sailor

import com.google.gson.JsonObject
import com.google.inject.Guice
import com.google.inject.Injector
import com.rabbitmq.client.AMQP
import io.elastic.api.Message
import spock.lang.Shared
import spock.lang.Specification

class CipherSpec extends Specification {

    @Shared
    def cipher;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        cipher = injector.getInstance(CipherWrapper.class);
    }

    def "should encrypt & decrypt strings"() {
        when:
        def result = cipher.encrypt("Hello%20world!")
        def decryptedResult = cipher.decrypt(result, new AMQP.BasicProperties())
        then:
        decryptedResult.toString() == "Hello world!"
    }

    def "should encrypt & decrypt objects"() {
        given:
        def content = new JsonObject()
        content.addProperty("property1", "Hello%20world!")
        when:
        def result = cipher.encryptMessageContent(content)
        def decryptedResult = cipher.decryptMessageContent(result, new AMQP.BasicProperties())
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
                "vSx5ntK2UdYh2Wjcdy8rgM7Yz5a/H8koXKtwNI0FL/Y9QiQFcUrtT4HJUkYXACNL",
                new AMQP.BasicProperties());
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
        def body = new JsonObject()
        body.addProperty("incomingProperty1", "incomingValue1")
        body.addProperty("incomingProperty2", "incomingValue2")

        def attachments = new JsonObject()
        attachments.addProperty("incomingAttachment1", "incomingAttachment1Content")
        attachments.addProperty("incomingAttachment2", "incomingAttachment2Content")

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
        def result = cipher.decryptMessage(encoded, new AMQP.BasicProperties());
        then:
        result.toString().equals(getMessage().toString())
    }

    def "should not fail in case of null message"() {
        when:
        cipher.decryptMessage(null, new AMQP.BasicProperties());
        then:
        notThrown(RuntimeException)
    }

    def "should encrypt & decrypt strings if message url decoding is skipped"() {
        given:
        def props = new AMQP.BasicProperties()
        props.headers = [
                (FeatureFlags.SKIP_MESSAGE_URL_DECODING): "1"
        ] as Map

        when:
        def result = cipher.encrypt("Hello%20world!")
        def decryptedResult = cipher.decrypt(result, props)
        then:
        decryptedResult.toString() == "Hello%20world!"
    }

    def "should encrypt & decrypt strings if message url decoding is skipped but message is not encoded"() {
        given:
        def props = new AMQP.BasicProperties()
        props.headers = [
                (FeatureFlags.SKIP_MESSAGE_URL_DECODING): "1"
        ] as Map

        when:
        def result = cipher.encrypt("Hello world")
        def decryptedResult = cipher.decrypt(result, props)
        then:
        decryptedResult.toString() == "Hello world"
    }
}
