package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.sailor.component.HelloWorldAction

class SailorSpec extends ApiAwareSpecification {

    def amqp = Mock(AMQPWrapperInterface)
    def componentBuilder = Mock(ComponentBuilder)

    def sailor;

    def setup() {

        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule())

        sailor = injector.getInstance(Sailor.class)
        sailor.setAMQP(amqp)
        sailor.setComponentBuilder(componentBuilder)
        sailor.setStep(TestUtils.createStep())
    }

    def "it should start correctly"() {
        setup:
        def component = new HelloWorldAction()

        when:
        sailor.start()

        then:
        1 * componentBuilder.build() >> component
        1 * amqp.connect()
        1 * amqp.subscribeConsumer(component)
    }

    def "it should fail and report exception"() {
        when:
        sailor.start()

        then:
        1 * componentBuilder.build() >> { throw new RuntimeException("OMG. I can't build the component")}
        1 * amqp.connect()
        1 * amqp.sendError({
            println(it.message)
            it.message == "OMG. I can't build the component"
        }, _, null)
    }
}
