package io.elastic.sailor.impl

import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.Constants
import io.elastic.sailor.MessagePublisher
import io.elastic.sailor.Utils
import spock.lang.Specification

import jakarta.json.Json

class ErrorPublisherImplSpec extends Specification {

    def messagePublisher = Mock(MessagePublisher)
    def crypto = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")
    def routingKey = "errorRoutingKey"

    def publisher

    def setup() {
        publisher = new ErrorPublisherImpl(messagePublisher, crypto, routingKey, false)
    }

    def "should publish error successfully - no protocol version"() {
        setup:
        def headers = Utils.buildAmqpProperties([:])

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().body(body).build()
        def bytes = crypto.encrypt(msg.toString(), MessageEncoding.BASE64)

        when:
        publisher.publish(new IllegalStateException("Ouch!"), headers, bytes)

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
        0 * _
    }

    def "should publish error successfully - protocol version 1 "() {
        setup:
        def headers = Utils.buildAmqpProperties([
                (Constants.AMQP_HEADER_PROTOCOL_VERSION): MessageEncoding.BASE64.protocolVersion
        ])

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().body(body).build()
        def bytes = crypto.encrypt(msg.toString(), MessageEncoding.BASE64)

        when:
        publisher.publish(new IllegalStateException("Ouch!"), headers, bytes)

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
        0 * _
    }

    def "should publish error successfully - protocol version 2"() {
        setup:
        def headers = Utils.buildAmqpProperties([
                (Constants.AMQP_HEADER_PROTOCOL_VERSION): MessageEncoding.UTF8.protocolVersion
        ])

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().body(body).build()
        def bytes = crypto.encrypt(msg.toString(), MessageEncoding.UTF8)

        when:
        publisher.publish(new IllegalStateException("Ouch!"), headers, bytes)

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
        0 * _
    }

    def "should publish error successfully - no http reply"() {
        setup:
        publisher.noErrorsReply = true
        def httpReplyRoutingKey = 'http_reply_routing_key'
        def properties = Utils.buildAmqpProperties([
                (Constants.AMQP_HEADER_REPLY_TO) : httpReplyRoutingKey
        ])

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().body(body).build()
        def bytes = crypto.encrypt(msg.toString(), MessageEncoding.BASE64)

        when:
        publisher.publish(new IllegalStateException("Ouch!"), properties, bytes)

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
                properties)
        0 * _
    }

    def "should publish error successfully to next step and as http reply"() {
        setup:
        def httpReplyRoutingKey = 'http_reply_routing_key'
        def properties = Utils.buildAmqpProperties([
                (Constants.AMQP_HEADER_REPLY_TO) : httpReplyRoutingKey
        ])

        def expectedHeaders = new HashMap<String, Object>(properties.getHeaders())
        expectedHeaders.put(Constants.AMQP_HEADER_ERROR_RESPONSE, true)

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().body(body).build()
        def bytes = crypto.encrypt(msg.toString(), MessageEncoding.BASE64)

        when:
        publisher.publish(new IllegalStateException("Ouch!"), properties, bytes)

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
                properties)

        1 * messagePublisher.publish(
                httpReplyRoutingKey,
                {
                    def error = crypto.decryptMessageContent(it, MessageEncoding.BASE64)
                    assert error.getString('name') == 'java.lang.IllegalStateException'
                    assert error.getString('message') == 'Ouch!'
                    assert error.getString('stack').startsWith('java.lang.IllegalStateException: Ouch!')

                    it
                },
                {
                    assert expectedHeaders == it.getHeaders()

                    it
                })
        0 * _
    }
}
