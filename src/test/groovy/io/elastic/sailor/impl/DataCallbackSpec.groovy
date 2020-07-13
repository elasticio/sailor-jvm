package io.elastic.sailor.impl

import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.ContainerContext
import io.elastic.sailor.ExecutionContext
import io.elastic.sailor.MessagePublisher
import io.elastic.sailor.MessageResolver
import io.elastic.sailor.TestUtils
import io.elastic.sailor.Utils
import spock.lang.Shared
import spock.lang.Specification

import javax.json.Json

class DataCallbackSpec extends Specification {
    def messagePublisher = Mock(MessagePublisher)
    def messageResolver = Mock(MessageResolver)

    @Shared
    CryptoServiceImpl crypto = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    ExecutionContext ctx = new ExecutionContext(
            TestUtils.createStep(), new Message.Builder().build(), Utils.buildAmqpProperties([:]), new ContainerContext())

    def callback = new DataCallback(ctx, messagePublisher, crypto, messageResolver, "aRoutingKey", false)


    def "should publish message successfully"() {
        setup:
        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def id = UUID.fromString("9d843898-2799-47bd-bede-123dd5d755ee")
        def msg = new Message.Builder().id(id).body(body).build()

        when:
        callback.receiveData(msg)

        then:
        1 * messagePublisher.publish(
                "aRoutingKey",
                {
                    def payload = crypto.decrypt(it, MessageEncoding.BASE64)
                    assert payload == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{},"body":{"hello":"world"},"attachments":{}}'
                    it
                },
                _)
    }

    def "should publish and externalize message successfully"() {
        setup:
        callback.emitLightweightMessage = true;
        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def id = UUID.fromString("9d843898-2799-47bd-bede-123dd5d755ee")
        def msg = new Message.Builder().id(id).body(body).build()

        when:
        callback.receiveData(msg)

        then:
        1 * messageResolver.externalize(_) >> new Message.Builder().id(id).headers(Json.createObjectBuilder().add("external-id", "123").build()).build().toJsonObject()
        1 * messagePublisher.publish(
                "aRoutingKey",
                {
                    def payload = crypto.decrypt(it, MessageEncoding.BASE64)
                    assert payload == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"external-id":"123"},"body":{},"attachments":{},"passthrough":{}}'
                    it
                },
                _)
    }


}
