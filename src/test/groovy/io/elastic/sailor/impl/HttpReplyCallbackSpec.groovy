package io.elastic.sailor.impl

import io.elastic.api.HttpReply
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.AMQPWrapperInterface
import io.elastic.sailor.CipherWrapper
import io.elastic.sailor.ExecutionContext
import io.elastic.sailor.Step
import spock.lang.Shared
import spock.lang.Specification

class HttpReplyCallbackSpec extends Specification {
    def amqp = Mock(AMQPWrapperInterface)
    @Shared
    def cipher = new CipherWrapper("testCryptoPassword", "iv=any16_symbols")

    def step = JSON.parseObject("{" +
            "\"id\":\"step_1\"," +
            "\"comp_id\":\"testcomponent\"," +
            "\"function\":\"test\"," +
            "\"snapshot\":{\"timestamp\":\"19700101\"}}")
    def ctx = new ExecutionContext(
            new Step(step), new Message.Builder().build(), Collections.emptyMap())

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
