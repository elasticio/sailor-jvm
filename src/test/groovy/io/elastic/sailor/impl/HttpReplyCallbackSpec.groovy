package io.elastic.sailor.impl

import io.elastic.api.HttpReply
import io.elastic.api.Message
import io.elastic.sailor.AmqpService
import io.elastic.sailor.ExecutionContext
import io.elastic.sailor.TestUtils
import io.elastic.sailor.Utils
import spock.lang.Shared
import spock.lang.Specification

class HttpReplyCallbackSpec extends Specification {
    def amqp = Mock(AmqpService)
    @Shared
    def cipher = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    def ctx = new ExecutionContext(
            TestUtils.createStep(), new Message.Builder().build(), Utils.buildAmqpProperties([:]))

    def callback = new HttpReplyCallback(ctx, amqp, cipher)

    def "should  successfully"() {
        setup:
        def reply = new HttpReply.Builder()
                .status(HttpReply.Status.ACCEPTED)
                .header("X-Powered-By", "elastic.io")
                .content(new ByteArrayInputStream("hello".getBytes()))
                .build()
        when:
        callback.receive(reply)

        then:
        1 * amqp.sendHttpReply(_, _)
    }
}
