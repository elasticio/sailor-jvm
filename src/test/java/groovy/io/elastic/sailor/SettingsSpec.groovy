package groovy.io.elastic.sailor

import spock.lang.Specification
import io.elastic.sailor.Settings

class SettingsSpec extends Specification{

    def getValidEnvVars(){
        def envVars  = new HashMap<String, String>();
        envVars.put("TASK", "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"function\":\"datas_and_errors\"}]}}");
        envVars.put("STEP_ID", "step_1");
        envVars.put("AMQP_URI", "amqp://guest:guest@localhost:5672");
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

    def "should not build settings if all are missing"() {
        when:
            def envVars  = new HashMap<String, String>();
            def settings = new Settings(envVars);
        then:
            IllegalArgumentException e = thrown()
            e.getMessage() == "AMQP_URI is missing"
    }

    def "should not build settings if some is missing"() {
        when:
            def envVars  = new HashMap<String, String>();
            envVars.put("AMQP_URI", "test");
            new Settings(envVars);
        then:
            IllegalArgumentException e = thrown()
            e.getMessage() == "LISTEN_MESSAGES_ON is missing"
    }

    def "should build settings successfully, get string and integer settings"() {
        given:

        when:
            def settings = new Settings(getValidEnvVars());
        then:
            notThrown(RuntimeException)
            settings.get("STEP_ID") == "step_1"
            settings.get("AMQP_URI") == "amqp://guest:guest@localhost:5672"
            settings.get("REBOUND_ROUTING_KEY") == "5559edd38968ec0736000003.test_exec.step_1.rebound"
            settings.get("COMPONENT_PATH") == "/spec/component/"
            settings.getInt("REBOUND_LIMIT") == 20
    }

    def "should throw error if some unknown property is accessed"() {
        given:

        when:
            def settings = new Settings(getValidEnvVars());
            settings.get("SOME_UNKNOWN_PROPERTY")
        then:
            RuntimeException e = thrown()
            e.getMessage() == "SOME_UNKNOWN_PROPERTY is not specified in settings"
    }

}
