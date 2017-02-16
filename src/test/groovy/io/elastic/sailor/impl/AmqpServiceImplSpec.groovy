package io.elastic.sailor.impl

import com.google.inject.Guice
import com.google.inject.Injector
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.elastic.api.Message
import io.elastic.sailor.ApiAwareSpecification
import io.elastic.sailor.SailorModule
import io.elastic.sailor.SailorTestModule
import io.elastic.sailor.impl.AmqpServiceImpl
import spock.lang.Shared

class AmqpServiceImplSpec extends ApiAwareSpecification {

    def exchange = "5527f0ea43238e5d5f000002_exchange"
    def dataRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.message"
    def errorRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.error"
    def snapshotRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.snapshot"
    def reboundRoutingKey = "5559edd38968ec0736000003.test_exec.step_1.rebound"

    def subscribeChannel = Mock(Channel)
    def publishChannel = Mock(Channel)

    @Shared
    def amqp;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule())

        amqp = injector.getInstance(AmqpServiceImpl.class)
    }

    def setup() {
        amqp.setSubscribeChannel(subscribeChannel)
        amqp.setPublishChannel(publishChannel)
    }

    def getOptions() {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .build();
    }

    def "Should send data with DATA_ROUTING_KEY"() {
        when:
        amqp.sendData(new String("some-content").getBytes(), getOptions());
        then:
        1 * publishChannel.basicPublish(exchange, dataRoutingKey, _, _)
    }

    def "Should send error with ERROR_ROUTING_KEY"() {
        when:
        amqp.sendError(new RuntimeException("Ouch"), getOptions(), new Message.Builder().build());
        then:
        1 * publishChannel.basicPublish(exchange, errorRoutingKey, _, _)
    }

    def "Should send snapshot with SNAPSHOT_ROUTING_KEY"() {
        when:
        amqp.sendSnapshot(new String("some-content").getBytes(), getOptions());
        then:
        1 * publishChannel.basicPublish(exchange, snapshotRoutingKey, _, _)
    }

    def "Should send rebound with REBOUND_ROUTING_KEY"() {
        when:
        amqp.sendRebound(new String("some-content").getBytes(), getOptions());
        then:
        1 * publishChannel.basicPublish(exchange, reboundRoutingKey, _, _)
    }

    def "Should send ack"() {
        when:
        amqp.ack(12345);
        then:
        1 * subscribeChannel.basicAck(12345, false)
    }
}
