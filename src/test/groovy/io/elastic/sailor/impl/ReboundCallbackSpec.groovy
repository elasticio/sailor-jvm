package io.elastic.sailor.impl

import io.elastic.api.Message
import io.elastic.sailor.*
import jakarta.json.JsonObject
import spock.lang.Specification
import spock.lang.Unroll

class ReboundCallbackSpec extends Specification{

    ExecutionContext ctx = new ExecutionContext(
            TestUtils.createStep(), new byte[0], new Message.Builder().build(), Utils.buildAmqpProperties([:]), new ContainerContext(), JsonObject.EMPTY_JSON_OBJECT)

    CryptoServiceImpl cipher = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    MessagePublisher publisher = Mock()

    def callback = new ReboundCallback(ctx, publisher, cipher, 5, 1500, "reboundRoutingKey")


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
