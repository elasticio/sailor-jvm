package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector

class SailorSpec extends ApiAwareSpecification {

    def amqp = Mock(AMQPWrapperInterface)

    def sailor;

    def setup() {

        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule())

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
