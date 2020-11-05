package io.elastic.sailor.impl

import com.google.inject.Guice
import com.google.inject.Injector
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.elastic.sailor.AmqpAwareModule
import io.elastic.sailor.ApiAwareSpecification
import io.elastic.sailor.SailorModule
import io.elastic.sailor.SailorTestModule
import spock.lang.Shared

class AmqpServiceImplSpec extends ApiAwareSpecification {

    def subscribeChannel = Mock(Channel)

    @Shared
    def amqp;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule(), new AmqpAwareModule())

        amqp = injector.getInstance(AmqpServiceImpl.class)
    }

    def setup() {
        amqp.subscribeChannel = subscribeChannel
    }

    def getOptions() {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .build();
    }

    def "Should send ack"() {
        when:
        amqp.ack(12345);
        then:
        1 * subscribeChannel.basicAck(12345, false)
    }
}
