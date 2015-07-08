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

    @SuppressWarnings("GroovyResultOfObjectAllocationIgnored")
    def "should not build settings if all are missing"() {
        when:
            def envVars  = new HashMap<String, String>();
            new Settings(envVars);
        then:
            IllegalArgumentException e = thrown()
            e.getMessage() == "AMQP_URI is missing"
    }

    @SuppressWarnings("GroovyResultOfObjectAllocationIgnored")
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
            // getInt
            settings.getInt("REBOUND_LIMIT") == 20
            // additional functions to get task, cfg, snapshot, trigger name
            settings.getTask().get("_id").getAsString() == "5559edd38968ec0736000003"
            settings.getStepId() == "step_1"
            settings.getCfg().toString() == "{\"uri\":\"546456456456456\"}"
            settings.getSnapshot() == null
            settings.getFunction() == "datas_and_errors"
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

    def "should throw error if function cannot be defined"() {
        given:

        when:
            def envVars = getValidEnvVars();
            envVars.put("STEP_ID", "step_3");
            def settings = new Settings(envVars);
            settings.getFunction()
        then:
            RuntimeException e = thrown()
            e.getMessage() == "Step step_3 is not found in task recipe"
    }

}
