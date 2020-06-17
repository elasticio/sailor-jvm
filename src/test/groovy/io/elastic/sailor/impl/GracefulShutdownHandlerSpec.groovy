package io.elastic.sailor.impl

import io.elastic.sailor.AmqpService
import spock.lang.Specification

class GracefulShutdownHandlerSpec extends Specification {
    def amqp = Mock(AmqpService)

    def "should not register shutdown handler on sailor shutdown" () {
        setup:
        def handler = new GracefulShutdownHandler(amqp, true)

        when:
        handler.prepareGracefulShutdown();

        then:
        handler.messagesProcessingCount == null

        when:
        handler.increment()

        then:
        handler.messagesProcessingCount == null

        when:
        handler.decrementAndExit()

        then:
        handler.messagesProcessingCount == null
    }

    def "should increment and decrement properly" () {
        setup:
        def handler = new GracefulShutdownHandler(amqp, false)

        when:
        handler.prepareGracefulShutdown();

        then:
        handler.messagesProcessingCount != null

        when:
        handler.increment()

        then:
        handler.messagesProcessingCount.get() == 1

        when:
        handler.increment()

        then:
        handler.messagesProcessingCount.get() == 2

        when:
        handler.increment()

        then:
        handler.messagesProcessingCount.get() == 3

        when:
        handler.decrementAndExit()

        then:
        handler.messagesProcessingCount.get() == 2


        when:
        handler.decrementAndExit()

        then:
        handler.messagesProcessingCount.get() == 1
    }
}
