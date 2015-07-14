package io.elastic.sailor
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import spock.lang.Specification

class AMPQSpec extends Specification {

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
        envVars.put("MESSAGE_CRYPTO_IV", "crypt123456");
        envVars.put("COMPONENT_PATH", "/spec/component/");
        return envVars;
    }

    def getOptions(){
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .build();
    }

    def settings = new Settings(getValidEnvVars())

    def "Should send data with DATA_ROUTING_KEY" () {
        given:
            def subscribeChannel = Mock(Channel)
            def publishChannel = Mock(Channel)
            def amqp = new AMQPWrapper(settings, subscribeChannel, publishChannel)
        when:
            amqp.sendData(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(settings.get("PUBLISH_MESSAGES_TO"), settings.get("DATA_ROUTING_KEY"), _, _)
    }

    def "Should send error with ERROR_ROUTING_KEY" () {
        given:
            def subscribeChannel = Mock(Channel)
            def publishChannel = Mock(Channel)
            def amqp = new AMQPWrapper(settings, subscribeChannel, publishChannel)
        when:
            amqp.sendError(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(settings.get("PUBLISH_MESSAGES_TO"), settings.get("ERROR_ROUTING_KEY"), _, _)
    }

    def "Should send snapshot with SNAPSHOT_ROUTING_KEY" () {
        given:
            def subscribeChannel = Mock(Channel)
            def publishChannel = Mock(Channel)
            def amqp = new AMQPWrapper(settings, subscribeChannel, publishChannel)
        when:
            amqp.sendSnapshot(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(settings.get("PUBLISH_MESSAGES_TO"), settings.get("SNAPSHOT_ROUTING_KEY"), _, _)
    }

    def "Should send rebound with REBOUND_ROUTING_KEY" () {
        given:
            def subscribeChannel = Mock(Channel)
            def publishChannel = Mock(Channel)
            def amqp = new AMQPWrapper(settings, subscribeChannel, publishChannel)
        when:
            amqp.sendRebound(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(settings.get("PUBLISH_MESSAGES_TO"), settings.get("REBOUND_ROUTING_KEY"), _, _)
    }

    def "Should send ack" () {
        given:
            def subscribeChannel = Mock(Channel)
            def publishChannel = Mock(Channel)
            def amqp = new AMQPWrapper(settings, subscribeChannel, publishChannel)
        when:
            amqp.ack(12345);
        then:
            1 * subscribeChannel.basicAck(12345, false)
    }
}
