package io.elastic.sailor.impl

import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.ComponentDescriptorResolver
import io.elastic.sailor.Constants
import io.elastic.sailor.ObjectStorage
import io.elastic.sailor.TestUtils
import spock.lang.Specification

import javax.json.Json

class MessageResolverImplSpec extends Specification {

    def crypto = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")
    def storage = Mock(ObjectStorage)

    def resolver

    def setup() {
        resolver = new MessageResolverImpl()
        resolver.setCryptoService(crypto)
        resolver.setObjectStorage(storage)
        resolver.setComponentDescriptorResolver(new ComponentDescriptorResolver())
        resolver.setStep(TestUtils.createStep("helloworldaction"))
    }

    def "should not resolve if object id not present"() {
        setup:
        def headers = Json.createObjectBuilder().build()
        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder().headers(headers).body(body).build()
        def encryptedMessage = crypto.encryptMessage(msg)

        when:
        def result = resolver.resolve(encryptedMessage.getBytes())

        then:
        JSON.stringify(result.toJsonObject()) == JSON.stringify(msg.toJsonObject())

    }

    def "should not resolve if autoResolveObjectReferences=false"() {
        setup:
        resolver.setStep(TestUtils.createStep("doNotAutoResolveObjectReferences"))

        def headers = Json.createObjectBuilder()
                .add(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID, "55e5eeb460a8e2070000001e")
                .build()

        def body = Json.createObjectBuilder().build()

        def msg = new Message.Builder().id(UUID.fromString("8c33707b-57cf-4001-86fe-4494cdf3d2a0"))
                .headers(headers)
                .body(body)
                .build()
        def encryptedMessage = crypto.encryptMessage(msg)

        when:
        def result = resolver.resolve(encryptedMessage.getBytes())

        then:
        JSON.stringify(result.toJsonObject()) == '{"id":"8c33707b-57cf-4001-86fe-4494cdf3d2a0","headers":{"x-ipaas-object-storage-id":"55e5eeb460a8e2070000001e"},"body":{},"attachments":{},"passthrough":{}}'
        JSON.stringify(result.toJsonObject()) == JSON.stringify(msg.toJsonObject())

    }

    def "should resolve the object by id successfully"() {
        setup:

        def headers = Json.createObjectBuilder()
                .add(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID, "55e5eeb460a8e2070000001e")
                .build()

        def body = Json.createObjectBuilder().build()

        def id = UUID.fromString("9d843898-2799-47bd-bede-123dd5d755ee")
        def msg = new Message.Builder().id(id).body(body).headers(headers).build()
        def encryptedMessage = crypto.encryptMessage(msg)


        when:
        def result = resolver.resolve(encryptedMessage.getBytes())

        then:
        1 * storage.getJsonObject("55e5eeb460a8e2070000001e") >> Json.createObjectBuilder().add("from", "storage").build()
        JSON.stringify(msg.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"55e5eeb460a8e2070000001e"},"body":{},"attachments":{},"passthrough":{}}'
        JSON.stringify(result.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{},"body":{"from":"storage"},"attachments":{},"passthrough":{}}'

    }


    def "should resolve with passthrough successfully"() {
        setup:

        def headers = Json.createObjectBuilder()
                .add(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID, "55e5eeb460a8e2070000001e")
                .build()

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def passthroughHeaders = Json.createObjectBuilder()
                .add(Constants.MESSAGE_HEADER_OBJECT_STORAGE_ID, "5b62c918fd98ea00112d5291")
                .build()

        def passthroughBody = Json.createObjectBuilder()
                .add("hello", "again")
                .build()

        def passthroughStep = new Message.Builder()
                .id(UUID.fromString("82317293-fcae-4d1f-9bc9-25aa8913f9f3"))
                .body(passthroughBody)
                .headers(passthroughHeaders)
                .build()

        def passthrough = Json.createObjectBuilder()
                .add("step_1", passthroughStep.toJsonObject())
                .build()

        def msg = new Message.Builder()
                .id(UUID.fromString("9d843898-2799-47bd-bede-123dd5d755ee"))
                .body(body)
                .headers(headers)
                .passthrough(passthrough)
                .build()
        def encryptedMessage = crypto.encryptMessage(msg)

        when:
        def result = resolver.resolve(encryptedMessage.getBytes())

        then:
        1 * storage.getJsonObject("55e5eeb460a8e2070000001e") >> Json.createObjectBuilder().add("from", "storage").build()
        1 * storage.getJsonObject("5b62c918fd98ea00112d5291") >> Json.createObjectBuilder().add("i am", "passthrough").build()
        JSON.stringify(msg.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"55e5eeb460a8e2070000001e"},"body":{"hello":"world"},"attachments":{},"passthrough":{"step_1":{"id":"82317293-fcae-4d1f-9bc9-25aa8913f9f3","headers":{"x-ipaas-object-storage-id":"5b62c918fd98ea00112d5291"},"body":{"hello":"again"},"attachments":{},"passthrough":{}}}}'
        JSON.stringify(result.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{},"body":{"from":"storage"},"attachments":{},"passthrough":{"step_1":{"body":{"i am":"passthrough"},"headers":{},"attachments":{},"id":"82317293-fcae-4d1f-9bc9-25aa8913f9f3"}}}'

    }


    def "should externalize successfully"() {
        setup:

        def headers = Json.createObjectBuilder().build()

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def msg = new Message.Builder()
                .id(UUID.fromString("9d843898-2799-47bd-bede-123dd5d755ee"))
                .body(body)
                .headers(headers)
                .build()

        def msgJson = msg.toJsonObject()

        when:
        def result = resolver.externalize(msgJson)

        then:
        1 * storage.postJsonObject(_) >> Json.createObjectBuilder().add("objectId", "58876284571c810019c78ef7").build()
        JSON.stringify(result) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"58876284571c810019c78ef7"},"body":{},"attachments":{},"passthrough":{}}'

    }

    def "should externalize with passthrough successfully"() {
        setup:

        def headers = Json.createObjectBuilder().build()

        def body = Json.createObjectBuilder()
                .add("hello", "world")
                .build()

        def passthroughBody = Json.createObjectBuilder()
                .add("hello", "again")
                .build()

        def passthroughHeaders = Json.createObjectBuilder()
                .add("foo", "bar")
                .build()

        def passthroughStep = new Message.Builder()
                .id(UUID.fromString("82317293-fcae-4d1f-9bc9-25aa8913f9f3"))
                .body(passthroughBody)
                .headers(passthroughHeaders)
                .build()

        def passthrough = Json.createObjectBuilder()
                .add("step_1", passthroughStep.toJsonObject())
                .build()

        def msg = new Message.Builder()
                .id(UUID.fromString("9d843898-2799-47bd-bede-123dd5d755ee"))
                .body(body)
                .headers(headers)
                .passthrough(passthrough)
                .build()

        def msgJson = msg.toJsonObject()

        when:
        def result = resolver.externalize(msgJson)

        then:
        1 * storage.postJsonObject(_) >> Json.createObjectBuilder().add("objectId", "58876284571c810019c78ef7").build()
        1 * storage.postJsonObject(_) >> Json.createObjectBuilder().add("objectId", "588763137d802200192b485c").build()
        JSON.stringify(result) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"58876284571c810019c78ef7"},"body":{},"attachments":{},"passthrough":{"step_1":{"id":"82317293-fcae-4d1f-9bc9-25aa8913f9f3","headers":{"foo":"bar","x-ipaas-object-storage-id":"588763137d802200192b485c"},"body":{},"attachments":{},"passthrough":{}}}}'

    }
}
