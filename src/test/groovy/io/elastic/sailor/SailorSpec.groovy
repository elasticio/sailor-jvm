package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.sailor.component.HelloWorldAction

class SailorSpec extends ApiAwareSpecification {

    def amqp = Mock(AmqpService)
    def componentBuilder = Mock(ModuleBuilder)

    def sailor;

    def setup() {

        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule())

        sailor = injector.getInstance(Sailor.class)
        sailor.setAMQP(amqp)
        sailor.setModuleBuilder(componentBuilder)
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
        1 * componentBuilder.build() >> { throw new RuntimeException("OMG. I can't build the component") }
        1 * amqp.connect()
        1 * amqp.sendError(
                {

                    it.message == "OMG. I can't build the component"
                },
                {
                    it.headers == ['stepId':'step_1',
                                   'compId':'5559edd38968ec0736000456',
                                   'userId':'5559edd38968ec0736000002',
                                   'taskId':'5559edd38968ec0736000003',
                                   'execId':'some-exec-id',
                                   'workspaceId': null,
                                   'containerId': 'container_123',
                                   'function': 'myFunction']

                },
                null)
    }
}
