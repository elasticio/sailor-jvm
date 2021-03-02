package io.elastic.sailor.impl

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import io.elastic.sailor.AmqpService
import io.elastic.sailor.Utils
import spock.lang.Specification

import java.util.concurrent.TimeoutException

class MessagePublisherImplSpec extends Specification {

    def amqp = Mock(AmqpService)
    def connection = Mock(Connection)
    def channel = Mock(Channel)

    def exchangeName = "targetExchange_12345"
    def routingKey = "data_routing_key"

    def headers = Utils.buildAmqpProperties(["flow_id": "flow_123"])

    def publisher = new MessagePublisherImpl(exchangeName, 3, 1, 35, true, amqp)

    def "should  publish and receive confirmation successfully"() {
        setup:
        def message = "Hello world".getBytes()

        when:
        publisher.publish(routingKey, message, headers)

        then:
        1 * amqp.getConnection() >> connection
        1 * connection.createChannel() >> channel
        1 * channel.confirmSelect()
        1 * channel.basicPublish(exchangeName, routingKey, headers, message)
        1 * channel.waitForConfirms(_) >> true
    }

    def "should  publish and retry"() {
        setup:
        def message = "Hello world".getBytes()

        when:
        publisher.publish(routingKey, message, headers)

        then:
        1 * amqp.getConnection() >> connection
        1 * connection.createChannel() >> channel
        1 * channel.confirmSelect()
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123"]), message)
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123", "retry": 1]), message)
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123", "retry": 2]), message)
        2 * channel.waitForConfirms(_) >> false
        1 * channel.waitForConfirms(_) >> true
    }

    def "should  publish and retry too many time"() {
        setup:
        def message = "Hello world".getBytes()

        when:
        publisher.publish(routingKey, message, headers)

        then:
        1 * amqp.getConnection() >> connection
        1 * connection.createChannel() >> channel
        1 * channel.confirmSelect()
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123"]), message)
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123", "retry": 1]), message)
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123", "retry": 2]), message)
        1 * channel.basicPublish(exchangeName, routingKey, Utils.buildAmqpProperties(["flow_id": "flow_123", "retry": 3]), message)
        4 * channel.waitForConfirms(_) >> false
        def e = thrown(IllegalStateException)
        e.message == "Failed to publish the message to a queue after 3 retries. The limit of 3 retries reached."
    }

    def "should  publish and handle thread interruption"() {
        setup:
        def message = "Hello world".getBytes()
        def waitError = new InterruptedException("Sorry, guy!");

        when:
        publisher.publish(routingKey, message, headers)

        then:
        1 * amqp.getConnection() >> connection
        1 * connection.createChannel() >> channel
        1 * channel.confirmSelect()
        1 * channel.basicPublish(exchangeName, routingKey, headers, message)
        1 * channel.waitForConfirms(_) >> { throw waitError }
        def e = thrown(RuntimeException)
        e.message == "java.lang.InterruptedException: Sorry, guy!"
        e.cause == waitError
    }

    def "should  publish and handle waiting timeout"() {
        setup:
        def message = "Hello world".getBytes()
        def waitError = new TimeoutException("It took too long!");

        when:
        publisher.publish(routingKey, message, headers)

        then:
        1 * amqp.getConnection() >> connection
        1 * connection.createChannel() >> channel
        1 * channel.confirmSelect()
        1 * channel.basicPublish(exchangeName, routingKey, headers, message)
        1 * channel.waitForConfirms(_) >> { throw waitError }
        def e = thrown(RuntimeException)
        e.message == "Waiting for publisher confirmation timed out"
    }

    def "should  publish and handle a non-confirm channel"() {
        setup:
        def message = "Hello world".getBytes()
        def waitError = new IllegalStateException("Channel is not configured to send confirmations");

        when:
        publisher.publish(routingKey, message, headers)

        then:
        1 * amqp.getConnection() >> connection
        1 * connection.createChannel() >> channel
        1 * channel.confirmSelect()
        1 * channel.basicPublish(exchangeName, routingKey, headers, message)
        1 * channel.waitForConfirms(_) >> { throw waitError }
        def e = thrown(RuntimeException)
        e == waitError
    }

    def "should  calculate the sleep duration correctly"() {
        expect:
        publisher.calculateSleepDuration(1) == 1
        publisher.calculateSleepDuration(2) == 2
        publisher.calculateSleepDuration(3) == 4
        publisher.calculateSleepDuration(4) == 8
        publisher.calculateSleepDuration(5) == 16
        publisher.calculateSleepDuration(6) == 32
        publisher.calculateSleepDuration(7) == 35
        publisher.calculateSleepDuration(8) == 35
        publisher.calculateSleepDuration(100) == 35
    }
}
