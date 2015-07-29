package io.elastic.sailor
import com.google.gson.JsonObject
import io.elastic.api.Message
import spock.lang.Specification

class CipherSpec extends Specification {

    def key = "testCryptoPassword";
    def iv ="iv=any16_symbols";

    def "should encrypt & decrypt strings"() {
        given:
            def content = "Hello world"
            def cipher = new CipherWrapper(key, iv)
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
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.encryptMessageContent(content)
            def decryptedResult = cipher.decryptMessageContent(result)
        then:
            decryptedResult.toString() == content.toString()
    }

    def "should throw error if failed to decrypt"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            cipher.decrypt("dsdasdsad");
        then: // TODO: should throw RuntimeException if input string is not JsonElement
            thrown(RuntimeException)
    }

    def "should decrypt JSON objects encrypted in Node.js"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.decryptMessageContent("vSx5ntK2UdYh2Wjcdy8rgM7Yz5a/H8koXKtwNI0FL/Y9QiQFcUrtT4HJUkYXACNL");
        then:
            result.get("body").toString() == "{\"someKey\":\"someValue\"}"
    }

    def "should encrypt JSON objects so that Node.js understands"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.encrypt(getMessage().toString());
        then:
            result == "wXTeSuonL1KvG7eKJ1Dk/hUHeLOhr7GMC1mGa7JyGQ9ZGg6AdjrKKn0ktoFMNVU77uB9dRd+tqqe0GNKlH8yuJrM2JWNdMbAWFHDLK5PvSRgL/negMTlmEnk/5/V5wharU8Qs9SW6rFI/E78Nkqlmqgwbd7ovHyzuOQIZj3kT4h6CW7S2fWJ559jpByhwXU1T8ZcGPOs4T+356AqYTXj8q2QgnkduKY7sNTrXNDsQUIZpm7tbBmMkoWuE6BXTitN/56TI2SVpo7TEQ/ef4c11fnrnCkpremZl4qPCCQcXD/47gMTSbSIydZCFQ584PE64pAwwn7UxloSen059tKKYF1BtGmBaqj97mHAL8izh3wsDoG8GuMRo2GhKopHnZTm";
    }

    def getMessage(){
        def body = new JsonObject()
        body.addProperty("incomingProperty1", "incomingValue1")
        body.addProperty("incomingProperty2", "incomingValue2")

        def attachments = new JsonObject()
        attachments.addProperty("incomingAttachment1", "incomingAttachment1Content")
        attachments.addProperty("incomingAttachment2", "incomingAttachment2Content")

        return new Message(body, attachments);
    }

    def "should encrypt message"() {
        given:
            def cipher = new CipherWrapper(key, iv)
        when:
            def result = cipher.encryptMessage(getMessage());
        then:
            result == "wXTeSuonL1KvG7eKJ1Dk/hUHeLOhr7GMC1mGa7JyGQ9ZGg6AdjrKKn0ktoFMNVU77uB9dRd+tqqe0GNKlH8yuJrM2JWNdMbAWFHDLK5PvSRgL/negMTlmEnk/5/V5wharU8Qs9SW6rFI/E78Nkqlmqgwbd7ovHyzuOQIZj3kT4h6CW7S2fWJ559jpByhwXU1T8ZcGPOs4T+356AqYTXj8q2QgnkduKY7sNTrXNDsQUIZpm7tbBmMkoWuE6BXTitN/56TI2SVpo7TEQ/ef4c11fnrnCkpremZl4qPCCQcXD/47gMTSbSIydZCFQ584PE64pAwwn7UxloSen059tKKYF1BtGmBaqj97mHAL8izh3wsDoG8GuMRo2GhKopHnZTm"
    }

    def "should decrypt message"() {
        given:
        def cipher = new CipherWrapper(key, iv)
        def encoded = "TOxRVfC2S4QDUzw6tzpoVNzi5ldNj+qGGrx2bMJLTn+0mgv3+xZNxMPHI5HdsTq+pBF3oXzgNmaFkXWGou0rPkyhSdpk/" +
                "ZjI6YciJrFhtOk9Bgh5ScAO/cZYChDertRLGjGNtm4/XTVdYCw5LBdyYDSoGfYt2K+09NtzoOGrK4KGAKhZm4BaEfCFTeGU" +
                "vXpSCaiUxaHxro7OpxvO1Y5EA/ZBJIXWjhTMyc8E0WF12+wCq1eByfl5WXvEOqksfk1FGOIjqxCn9UEo995Y2f0YMA=="
        when:
        def result = cipher.decryptMessage(encoded);
        then:
        result.toString().equals(getMessage().toString())
    }

    def "should not fail in case of null message"() {
        given:
        def cipher = new CipherWrapper(key, iv)
        when:
        cipher.decryptMessage(null);
        then:
        notThrown(RuntimeException)
    }
}
