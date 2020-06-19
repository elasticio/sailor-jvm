package io.elastic.sailor.impl

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.ComponentDescriptorResolver
import io.elastic.sailor.Constants
import io.elastic.sailor.TestUtils
import org.junit.Rule
import spock.lang.Specification

import javax.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo

class MessageResolverImplSpec extends Specification {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule(12345);

    def crypto = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    def resolver

    def setup() {
        resolver = new MessageResolverImpl()
        resolver.setCryptoService(crypto)
        resolver.setObjectStorageUri("http://localhost:12345")
        resolver.setObjectStorageToken("secret_token")
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

    def "should not resolve if object storage id is not set"() {
        setup:
        resolver.setObjectStorageUri(null)
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
        JSON.stringify(msg.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"55e5eeb460a8e2070000001e"},"body":{},"attachments":{},"passthrough":{}}'
        JSON.stringify(result.toJsonObject()) == JSON.stringify(msg.toJsonObject())

    }

    def "should not resolve if object storage auth token is not set"() {
        setup:
        resolver.setObjectStorageToken(null)
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
        JSON.stringify(msg.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"55e5eeb460a8e2070000001e"},"body":{},"attachments":{},"passthrough":{}}'
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

        driver.addExpectation(
                onRequestTo("/objects/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.GET)
                        .withHeader("Authorization", "Bearer secret_token"),
                giveResponse('{"from":"storage"}', 'application/json')
                        .withStatus(200));

        when:
        def result = resolver.resolve(encryptedMessage.getBytes())

        then:
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

        driver.addExpectation(
                onRequestTo("/objects/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.GET)
                        .withHeader("Authorization", "Bearer secret_token"),
                giveResponse('{"from":"storage"}', 'application/json').withStatus(200));

        driver.addExpectation(
                onRequestTo("/objects/5b62c918fd98ea00112d5291")
                        .withMethod(ClientDriverRequest.Method.GET)
                        .withHeader("Authorization", "Bearer secret_token"),
                giveResponse('{"i am":"passthough"}', 'application/json').withStatus(200));

        when:
        def result = resolver.resolve(encryptedMessage.getBytes())

        then:
        JSON.stringify(msg.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{"x-ipaas-object-storage-id":"55e5eeb460a8e2070000001e"},"body":{"hello":"world"},"attachments":{},"passthrough":{"step_1":{"id":"82317293-fcae-4d1f-9bc9-25aa8913f9f3","headers":{"x-ipaas-object-storage-id":"5b62c918fd98ea00112d5291"},"body":{"hello":"again"},"attachments":{},"passthrough":{}}}}'
        JSON.stringify(result.toJsonObject()) == '{"id":"9d843898-2799-47bd-bede-123dd5d755ee","headers":{},"body":{"from":"storage"},"attachments":{},"passthrough":{"step_1":{"body":{"i am":"passthough"},"headers":{},"attachments":{},"id":"82317293-fcae-4d1f-9bc9-25aa8913f9f3"}}}'

    }
}
