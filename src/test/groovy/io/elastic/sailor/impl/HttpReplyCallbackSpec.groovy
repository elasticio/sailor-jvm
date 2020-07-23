package io.elastic.sailor.impl

import io.elastic.api.HttpReply
import io.elastic.api.Message
import io.elastic.sailor.*
import spock.lang.Shared
import spock.lang.Specification

class HttpReplyCallbackSpec extends Specification {
    def publisher = Mock(MessagePublisher)
    @Shared
    def cipher = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    def headers = Utils.buildAmqpProperties(["reply_to": "reply_queue_123"])

    def ctx = new ExecutionContext(
            TestUtils.createStep(), new byte[0], new Message.Builder().build(), headers, new ContainerContext())

    def callback = new HttpReplyCallback(ctx, publisher, cipher)

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
        1 * publisher.publish("reply_queue_123", _, {
            def headers = it.getHeaders()
            assert headers.size() == 13
            assert headers.stepId == 'step_1'
            assert headers.reply_to == 'reply_queue_123'
            assert headers.protocolVersion == 1
            it
        })
    }
}
