package io.elastic.sailor.impl

import io.elastic.sailor.AmqpService
import org.apache.http.impl.client.CloseableHttpClient
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class GracefulShutdownHandlerSpec extends Specification {
    def amqp = Mock(AmqpService)
    def httpClient = Mock(CloseableHttpClient)

    def "should increment and decrement properly" () {
        setup:
        def handler = new GracefulShutdownHandler(amqp, httpClient)

        when:
        handler.prepareGracefulShutdown();

        then:
        1 * amqp.cancelConsumer()
        handler.messagesProcessingCount.get() == 0

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
        handler.decrement()

        then:
        handler.messagesProcessingCount.get() == 2


        when:
        handler.decrement()

        then:
        handler.messagesProcessingCount.get() == 1

        when:
        handler.decrement()

        then:
        handler.messagesProcessingCount.get() == 0
    }

    def "should not close http client while tasks are running"() {
        given:
        def handler = new GracefulShutdownHandler(amqp, httpClient)
        def clientClosed = new AtomicBoolean(false)

        // When close is called, set the flag
        httpClient.close() >> { clientClosed.set(true) }

        // Start a task that will "run" for a while
        handler.increment()

        when:
        // Start shutdown in a separate thread
        new Thread({ handler.prepareGracefulShutdown() }).start()

        // Give shutdown a moment to start and cancel the consumer
        Thread.sleep(100)

        // At this point, prepareGracefulShutdown should be blocked on exitSignal.await()

        then:
        // The client should NOT be closed yet, because one task is still running
        !clientClosed.get()

        when:
        // Now, the task finishes
        handler.decrement()

        // Give shutdown thread time to unblock and close resources
        Thread.sleep(100)

        then:
        // NOW the client should be closed
        clientClosed.get()
        1 * amqp.disconnect()
    }
}