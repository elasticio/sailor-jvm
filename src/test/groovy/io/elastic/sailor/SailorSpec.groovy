package io.elastic.sailor

import com.google.inject.AbstractModule
import com.google.inject.Guice
import io.elastic.api.Function
import io.elastic.sailor.component.HelloWorldAction

class SailorSpec extends ApiAwareSpecification {

    def amqp = Mock(AmqpService)
    def errorPublisher = Mock(ErrorPublisher)
    def function = Mock(Function)
    def injector

    def sailor;

    def setup() {
        Sailor.gracefulShutdownHandler = null
        injector = Guice.createInjector(new SailorModule(), new SailorTestModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(AmqpService.class).toInstance(amqp);
                bind(ErrorPublisher.class).toInstance(errorPublisher);
            }
        })

        sailor = injector.getInstance(Sailor.class)
        sailor.setFunction(function)
        sailor.setStep(TestUtils.createStep())
    }

    def "it should start correctly"() {
        setup:
        def component = new HelloWorldAction()

        when:
        sailor.start(injector)

        then:
        1 * amqp.connect()
        1 * amqp.createSubscribeChannel()
        1 * function.init(_)
        1 * amqp.subscribeConsumer()
        Sailor.gracefulShutdownHandler != null
    }

    def "it should fail and report exception"() {
        when:
        sailor.start(injector)

        then:
        1 * amqp.connect()
        1 * amqp.createSubscribeChannel()
        0 * amqp.subscribeConsumer()
        1 * function.init(_) >> { throw new RuntimeException("OMG. I can't start up the component") }
        1 * errorPublisher.publish(
                {

                    it.message == "OMG. I can't start up the component"
                },
                {
                    it.headers == ['stepId':'step_1',
                                   'compId':'5559edd38968ec0736000456',
                                   'userId':'5559edd38968ec0736000002',
                                   'taskId':'5559edd38968ec0736000003',
                                   'execId':'some-exec-id',
                                   'workspaceId': 'workspace_123',
                                   'containerId': 'container_123',
                                   'function': 'myFunction']

                },
                null)
    }
}
