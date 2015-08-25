package io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.api.Message
import spock.lang.Specification

class SailorSpec extends Specification {

    def amqp = Mock(AMQPWrapperInterface)

    def "it should not start if AMQP url is missing"() {
        when:
        def sailor = new Sailor();
        sailor.setAMQP(amqp);
        sailor.start();
        then:
        RuntimeException e = thrown()
        e.getMessage() == "Env var 'AMQP_URI' is required"

        then:
        amqp.connect()
    }


    def "should throw exception if failed to connect to AMQP"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456")
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000")
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "src/test/java/io/elastic/sailor/component")
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@some-rabbit-server.com:5672")
        when:
        def sailor = new Sailor();
        sailor.init();
        sailor.start();
        then:
        RuntimeException e = thrown()
        e.getMessage() == "java.net.UnknownHostException: some-rabbit-server.com"
    }

    def "should throw exception if component is not found"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456")
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000")
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "src/test/java/groovy/io/elastic")
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@some-rabbit-server.com:5672")
        when:
        def sailor = new Sailor();
        sailor.init();
        then:
        RuntimeException e = thrown()
        e.getMessage().contains("component.json is not found in")
    }

    def checkOutgoingHeaders(HashMap headers, String function) {
        return headers.get("execId") == "exec1" &&
                headers.get("taskId") == "task2" &&
                headers.get("userId") == "user3" &&
                headers.get("stepId") == "step_1" &&
                headers.get("compId") == "testcomponent" &&
                headers.get("function") == function;
    }

    def "should process message with TestAction and send responses to AMQP"() {
        setup:
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456")
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000")
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "src/test/java/io/elastic/sailor/component")
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@127.0.0.1:5672")
        System.setProperty(ServiceSettings.ENV_VAR_STEP_ID, "step_1");
        System.setProperty(ServiceSettings.ENV_VAR_TASK, "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");

        // message
        def body = new JsonObject();
        body.addProperty("someProperty", "someValue");
        def attachments = new JsonObject();
        attachments.addProperty("attachment1", "attachmentContent");
        def message = new Message(body, attachments);

        // headers
        def headers = new HashMap();
        headers.put("execId", "exec1");
        headers.put("taskId", "task2")
        headers.put("userId", "user3");
        when:
        def sailor = new Sailor();
        sailor.init();
        sailor.setAMQP(amqp);
        sailor.processMessage(message, headers, 12345);
        then:
        1 * amqp.sendData(_, _)
        1 * amqp.sendSnapshot(_, _)
        1 * amqp.sendRebound(_, _)
        1 * amqp.sendError(_, _)
        1 * amqp.ack(12345)
    }
}
