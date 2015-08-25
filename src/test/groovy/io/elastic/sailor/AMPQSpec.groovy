package io.elastic.sailor

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import spock.lang.Specification

class AMPQSpec extends Specification {
    
    def exchange = "5527f0ea43238e5d5f000002_exchange"
    def dataRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.message"
    def errorRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.error"
    def snapshotRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.snapshot"
    def reboundRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.rebound"

    def subscribeChannel = Mock(Channel)
    def publishChannel = Mock(Channel)
    def amqp

    def setup() {
        System.setProperty(ServiceSettings.ENV_VAR_TASK, "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");
        System.setProperty(ServiceSettings.ENV_VAR_STEP_ID, "step_1");
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@some-rabbit-server.com:5672");
        System.setProperty(ServiceSettings.ENV_VAR_LISTEN_MESSAGES_ON, "5559edd38968ec0736000003:test_exec:step_1:messages");
        System.setProperty(ServiceSettings.ENV_VAR_PUBLISH_MESSAGES_TO, exchange);
        System.setProperty(ServiceSettings.ENV_VAR_DATA_ROUTING_KEY, dataRoutingKey);
        System.setProperty(ServiceSettings.ENV_VAR_ERROR_ROUTING_KEY, errorRoutingKey);
        System.setProperty(ServiceSettings.ENV_VAR_SNAPSHOT_ROUTING_KEY, snapshotRoutingKey);
        System.setProperty(ServiceSettings.ENV_VAR_REBOUND_ROUTING_KEY,  reboundRoutingKey);
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, "crypt123456");
        System.setProperty(ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV, "0000000000000000");
        System.setProperty(ServiceSettings.ENV_VAR_COMPONENT_PATH, "/spec/component/");

        amqp = new AMQPWrapper()
        amqp.setSubscribeChannel(subscribeChannel)
        amqp.setPublishChannel(publishChannel)
    }

    def setupSpec() {
    }

    def getOptions(){
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .build();
    }

    def "Should send data with DATA_ROUTING_KEY" () {
        when:
            amqp.sendData(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(exchange, dataRoutingKey, _, _)
    }

    def "Should send error with ERROR_ROUTING_KEY" () {
        when:
            amqp.sendError(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(exchange, errorRoutingKey, _, _)
    }

    def "Should send snapshot with SNAPSHOT_ROUTING_KEY" () {
        when:
            amqp.sendSnapshot(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(exchange, snapshotRoutingKey, _, _)
    }

    def "Should send rebound with REBOUND_ROUTING_KEY" () {
        when:
            amqp.sendRebound(new String("some-content").getBytes(), getOptions());
        then:
            1 * publishChannel.basicPublish(exchange, reboundRoutingKey, _, _)
    }

    def "Should send ack" () {
        when:
            amqp.ack(12345);
        then:
            1 * subscribeChannel.basicAck(12345, false)
    }
}
