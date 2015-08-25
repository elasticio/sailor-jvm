package io.elastic.sailor

import com.google.gson.JsonObject
import spock.lang.Ignore
import spock.lang.Specification

import io.elastic.api.Message;

class SailorSpec extends Specification {

    def amqp = Mock(AMQPWrapperInterface)

    def getValidEnvVars() {
        def envVars = new HashMap<String, String>();
        envVars.put("TASK", "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");
        envVars.put("STEP_ID", "step_1");
        envVars.put("AMQP_URI", "amqp://guest:guest@some-rabbit-server.com:5672");
        envVars.put("LISTEN_MESSAGES_ON", "5559edd38968ec0736000003:test_exec:step_1:messages");
        envVars.put("PUBLISH_MESSAGES_TO", "5527f0ea43238e5d5f000002_exchange");
        envVars.put("DATA_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.message");
        envVars.put("ERROR_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.error");
        envVars.put("SNAPSHOT_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.snapshot");
        envVars.put("REBOUND_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.rebound");
        envVars.put("MESSAGE_CRYPTO_PASSWORD", "crypt123456");
        envVars.put("MESSAGE_CRYPTO_IV", "0000000000000000");
        envVars.put("COMPONENT_PATH", "src/test/java/io/elastic/sailor/component/");
        return envVars;
    }

    def "it should not init if AMQP url is missing"() {

        given:
        def envVars = new HashMap<String, String>();
        when:
        def sailor = new Sailor();
        sailor.init(envVars);
        then:
        RuntimeException e = thrown()
        e.getMessage() == "AMQP_URI is missing"
    }

    @Ignore
    def "should init successfully if settings are valid"() {
        given:
        def envVars = getValidEnvVars();
        when:
        def sailor = new Sailor();
        sailor.init(envVars);
        then:
        notThrown(RuntimeException)
    }

    def "should throw exception if failed to connect to AMQP"() {
        setup:
        def envVars = getValidEnvVars();
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456")
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000")
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "src/test/java/io/elastic/sailor/component")
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@some-rabbit-server.com:5672")
        when:
        def sailor = new Sailor();
        sailor.init(envVars);
        sailor.start();
        then:
        RuntimeException e = thrown()
        e.getMessage() == "java.net.UnknownHostException: some-rabbit-server.com"
    }

    def "should throw exception if component is not found"() {
        setup:
        def envVars = getValidEnvVars();
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456")
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000")
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "src/test/java/groovy/io/elastic")
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@some-rabbit-server.com:5672")
        when:
        def sailor = new Sailor();
        sailor.init(envVars);
        then:
        RuntimeException e = thrown()
        e.getMessage().contains("component.json is not found in")
    }

    def "should not throw exception if component is found"() {
        setup:
        def envVars = getValidEnvVars();
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456")
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000")
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "src/test/java/io/elastic/sailor/component")
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@127.0.0.1:5672")

        when:
        def sailor = new Sailor();
        sailor.init(envVars);
        then:
        notThrown(RuntimeException)
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
        def envVars = getValidEnvVars();
        envVars.put("TASK", "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");

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
        sailor.init(envVars);
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
