package io.elastic.sailor.impl

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import io.elastic.api.JSON
import org.junit.Rule
import spock.lang.Specification

import javax.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.*

class ObjectStorageImplSpec extends Specification {

    @Rule
    public ClientDriverRule driver = new ClientDriverRule(12345);

    def crypto = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    def storage

    def setup() {
        storage = new ObjectStorageImpl()
        storage.setCryptoService(crypto)
        storage.setObjectStorageUri("http://localhost:12345/")
        storage.setObjectStorageToken("secret_token")
    }

    def "should not resolve if object storage uri is not set"() {
        setup:
        storage.setObjectStorageUri(null)

        when:
        def result = storage.getJsonObject("55e5eeb460a8e2070000001e")

        then:
        result == null

    }

    def "should not resolve if object storage auth token is not set"() {
        setup:
        storage.setObjectStorageToken(null)

        when:
        def result = storage.getJsonObject("55e5eeb460a8e2070000001e")

        then:
        result == null

    }

    def "should resolve the object by id successfully"() {
        setup:
        def object = '{"from":"storage"}'
        def storageObjectEncrypted = crypto.encrypt(object, MessageEncoding.UTF8)

        driver.addExpectation(
                onRequestTo("/objects/55e5eeb460a8e2070000001e")
                        .withMethod(ClientDriverRequest.Method.GET)
                        .withHeader("Authorization", "Bearer secret_token"),
                giveResponseAsBytes(new ByteArrayInputStream(storageObjectEncrypted), 'application/pdf')
                        .withStatus(200));

        when:
        def result = storage.getJsonObject("55e5eeb460a8e2070000001e")

        then:
        JSON.stringify(result) == object

    }

    def "should not post object if  storage uri is not set"() {
        setup:
        storage.setObjectStorageUri(null)

        when:
        def result = storage.postJsonObject(Json.createObjectBuilder().build())

        then:
        result == null

    }


    def "should not post object if  storage auth token is not set"() {
        setup:
        storage.setObjectStorageToken(null)

        when:
        def result = storage.postJsonObject(Json.createObjectBuilder().build())

        then:
        result == null

    }

    def "should post object successfully"() {
        setup:
        def response = Json.createObjectBuilder()
                .add("contentLength", "64")
                .add("contentType", "64")
                .add("contentType", "application/octet-stream")
                .add("createdAt", "1592570365121")
                .add("md5", "277876380e5b4feb985184f836cf83f8")
                .add("objectId", "b7cd53b1-5c9a-4895-a6f1-0f60ad48dfa1")
                .add("metadata", Json.createArrayBuilder().build())
                .build()

        driver.addExpectation(
                onRequestTo("/objects/")
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withHeader("Authorization", "Bearer secret_token"),
                giveResponse(JSON.stringify(response), 'application/json')
                        .withStatus(200));

        when:
        def result = storage.postJsonObject(Json.createObjectBuilder().add("happy", "hibernating").build())

        then:
        result == response

    }
}
