package groovy.io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.sailor.AMQPWrapperInterface
import spock.lang.Specification

import io.elastic.sailor.Sailor
import io.elastic.api.Message;

class SailorSpec extends Specification {

    def getValidEnvVars(){
        def envVars  = new HashMap<String, String>();
        envVars.put("TASK", "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"function\":\"datas_and_errors\"}]}}");
        envVars.put("STEP_ID", "step_1");
        envVars.put("AMQP_URI", "amqp://guest:guest@some-rabbit-server.com:5672");
        envVars.put("LISTEN_MESSAGES_ON", "5559edd38968ec0736000003:test_exec:step_1:messages");
        envVars.put("PUBLISH_MESSAGES_TO", "5527f0ea43238e5d5f000002_exchange");
        envVars.put("DATA_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.message");
        envVars.put("ERROR_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.error");
        envVars.put("SNAPSHOT_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.snapshot");
        envVars.put("REBOUND_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.rebound");
        envVars.put("MESSAGE_CRYPTO_PASSWORD", "crypt123456");
        envVars.put("COMPONENT_PATH", "/spec/component/");
        return envVars;
    }

    def "should not init if settings are invalid"() {

        given:
            def envVars  = new HashMap<String, String>();
        when:
            def sailor  = new Sailor();
            sailor.init(envVars);
        then:
            RuntimeException e = thrown()
            e.getMessage() == "AMQP_URI is missing"
    }

    def "should init successfully if settings are invalid"() {
        given:
            def envVars  = getValidEnvVars();
        when:
            def sailor  = new Sailor();
            sailor.init(envVars);
        then:
            notThrown(RuntimeException)
    }

    def "should throw exception if failed to connect to AMQP"() {
        given:
            def envVars  = getValidEnvVars();
        when:
            def sailor  = new Sailor();
            sailor.init(envVars);
            sailor.start();
        then:
            RuntimeException e = thrown()
            e.getMessage() == "java.net.UnknownHostException: some-rabbit-server.com"
    }

    def "should throw exception if component is not found"() {
        given:
            def envVars = getValidEnvVars();
            envVars.put("AMQP_URI", "amqp://guest:guest@127.0.0.1:5672");
        when:
            def sailor = new Sailor();
            sailor.init(envVars);
            sailor.start();
        then:
            RuntimeException e = thrown()
            e.getMessage() == "component.json is not found"
    }

    def "should not throw exception if component is found"() {
        given:
            def envVars = getValidEnvVars();
            envVars.put("AMQP_URI", "amqp://guest:guest@127.0.0.1:5672");
            envVars.put("COMPONENT_PATH", "src/test/java/groovy/io/elastic/sailor/component");
        when:
            def sailor = new Sailor();
            sailor.init(envVars);
            sailor.start();
        then:
            notThrown(RuntimeException)
    }

    def "should process message and send responses to AMQP"() {
        given:
            def envVars = getValidEnvVars();
            envVars.put("AMQP_URI", "amqp://guest:guest@127.0.0.1:5672");
            envVars.put("COMPONENT_PATH", "src/test/java/groovy/io/elastic/sailor/component");
            def amqp = Mock(AMQPWrapperInterface)

            def body = new JsonObject();
            body.addProperty("someProperty", "someValue");
            def attachments = new JsonObject();
            attachments.addProperty("attachment1", "attachmentContent");
            def message = new Message(body, attachments);
        when:
            def sailor = new Sailor();
            sailor.init(envVars);
            sailor.setAMQP(amqp);
            sailor.processMessage(message);
        then:
            1 * amqp.sendData(_)
            1 * amqp.sendError(_)
            1 * amqp.sendRebound(_)
    }
}
