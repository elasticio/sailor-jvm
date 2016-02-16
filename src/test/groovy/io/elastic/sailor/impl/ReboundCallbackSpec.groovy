package io.elastic.sailor.impl

import com.google.gson.JsonParser
import io.elastic.api.Message
import io.elastic.sailor.AMQPWrapperInterface
import io.elastic.sailor.CipherWrapper
import io.elastic.sailor.ExecutionContext
import io.elastic.sailor.Step
import spock.lang.Specification
import spock.lang.Unroll

class ReboundCallbackSpec extends Specification{

    def step = new JsonParser().parse("{" +
            "\"id\":\"step_1\"," +
            "\"compId\":\"testcomponent\"," +
            "\"function\":\"test\"," +
            "\"snapshot\":{\"timestamp\":\"19700101\"}}")
    ExecutionContext ctx = new ExecutionContext(
            new Step(step), new Message.Builder().build(), Collections.emptyMap())

    CipherWrapper cipher = new CipherWrapper("testCryptoPassword", "iv=any16_symbols")

    AMQPWrapperInterface amqp = Mock()

    def callback = new ReboundCallback(ctx, amqp, cipher, 5, 1500)


    @Unroll
    def "should calculate rebound expiration=#result properly for iteration #iteration"() {

        expect:
        callback.getReboundExpiration(iteration) == result

        where:
        iteration | result
        1 | 1500
        2 | 3000
        3 | 6000

    }

    def "should make rebound options properly"() {

        when:
        def result = callback.makeReboundOptions(['my-header':'my-value'], 1500)

        then:
        result.contentType == 'application/json'
        result.contentEncoding == 'utf8'
        result.expiration == '1500'
        result.headers == ['my-header':'my-value']

    }
}
