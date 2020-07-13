package io.elastic.sailor.impl

import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.MessagePublisher
import io.elastic.sailor.Utils
import spock.lang.Specification

import javax.json.Json

class ErrorPublisherImplSpec extends Specification {

    def messagePublisher = Mock(MessagePublisher)
    def crypto = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")
    def routingKey = "errorRoutingKey"
    def headers = Utils.buildAmqpProperties(["flow_id": "flow_123"])

    def publisher = new ErrorPublisherImpl(messagePublisher, crypto, routingKey)

    def "should  publish error successfully"() {
        setup:

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().body(body).build();

        when:
        publisher.publish(new IllegalStateException("Ouch!"), headers, msg)

        then:
        1 * messagePublisher.publish(
                routingKey,
                {
                    def payload = JSON.parseObject(new String(it))
                    def errorString = crypto.decrypt(
                            payload.getString("error").getBytes(), MessageEncoding.BASE64)
                    def error = JSON.parseObject(errorString)
                    assert error.getString('name') == 'java.lang.IllegalStateException'
                    assert error.getString('message') == 'Ouch!'
                    assert error.getString('stack').startsWith('java.lang.IllegalStateException: Ouch!')

                    def errorInputString = crypto.decrypt(
                            payload.getString("errorInput").getBytes(), MessageEncoding.BASE64)
                    def errorInput = JSON.parseObject(errorInputString)
                    assert JSON.stringify(errorInput.get('body')) == '{"hello":"world"}'

                    it
                },
                headers)
    }
}
