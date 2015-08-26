package io.elastic.sailor

import spock.lang.Ignore
import spock.lang.Specification

class SailorSpec extends Specification {

    def amqp = Mock(AMQPWrapperInterface)

    @Ignore
    def "it should not start if AMQP url is missing"() {
        setup:
        def componentResolver = new ComponentResolver("src/test/java/io/elastic/sailor/component");
        def cipher = new CipherWrapper("crypt123456", "0000000000000000");
        def sailor = new Sailor(componentResolver, cipher, amqp);

        when:

        sailor.start();
        then:
        RuntimeException e = thrown()
        e.getMessage() == "Env var 'AMQP_URI' is required"

        then:
        amqp.connect()
    }

    @Ignore
    def "it should not start if AMQP exchange to listen to is missing"() {
        setup:
        def componentResolver = new ComponentResolver("src/test/java/io/elastic/sailor/component");
        def cipher = new CipherWrapper("crypt123456", "0000000000000000");
        def sailor = new Sailor(componentResolver, cipher, amqp);
        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@some-rabbit-server.com:5672")

        when:

        sailor.start();
        then:
        RuntimeException e = thrown()
        e.getMessage() == "Env var 'LISTEN_MESSAGES_ON' is required"

        then:
        amqp.connect()
    }

    def "should process message with TestAction and send responses to AMQP"() {
        setup:

        def componentResolver = new ComponentResolver("src/test/java/io/elastic/sailor/component");
        def cipher = new CipherWrapper("crypt123456", "0000000000000000");
        def sailor = new Sailor(componentResolver, cipher, amqp);
        sailor.setAMQP(amqp);

        System.setProperty(ServiceSettings.ENV_VAR_AMQP_URI, "amqp://guest:guest@127.0.0.1:5672")
        System.setProperty(ServiceSettings.ENV_VAR_LISTEN_MESSAGES_ON, "5559edd38968ec0736000003:test_exec:step_1:messages")


        when:
        sailor.start();

        then:
        1 * amqp.connect("amqp://guest:guest@127.0.0.1:5672")
        1 * amqp.subscribeConsumer("5559edd38968ec0736000003:test_exec:step_1:messages", _)
    }
}
