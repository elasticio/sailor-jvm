package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import spock.lang.Specification

class SailorSpec extends Specification {

    def amqp = Mock(AMQPWrapperInterface)

    def sailor;

    def setup() {
        Injector injector = Guice.createInjector(new SailorModule(), new TestModule())

        sailor = injector.getInstance(Sailor.class)
        sailor.setAMQP(amqp)
    }

    def "it should start correctly"() {
        when:
        sailor.start();

        then:
        1 * amqp.connect()
        1 * amqp.subscribeConsumer()
    }
}
