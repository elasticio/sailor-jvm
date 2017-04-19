package io.elastic.sailor

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.component.HelloWorldAction
import io.elastic.sailor.impl.CryptoServiceImpl
import io.elastic.sailor.impl.MessageConsumer
import spock.lang.Shared
import spock.lang.Specification

import javax.json.Json

class MessageConsumerSpec extends Specification {

    Channel channel = Mock()
    MessageProcessor processor = Mock()

    @Shared
    CryptoServiceImpl cipher = new CryptoServiceImpl("testCryptoPassword", "iv=any16_symbols")

    def consumer

    @Shared
    def amqpProperties

    @Shared
    def headers

    def consumerTag = "tag12345"

    def envelope = new Envelope(123456, false, "test", "test2")

    @Shared
    def encryptedMessage

    def component

    def setupSpec() {

        headers = ["execId": "exec1", "taskId": "task2", "userId": "user3"] as Map

        amqpProperties = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .build();

        def body = Json.createObjectBuilder()
                .add("content", "Hello world!")
                .build()

        def msg = new Message.Builder().body(body).build();
        encryptedMessage = cipher.encryptMessage(msg)
    }

    def setup() {
        component = new HelloWorldAction()
        consumer = new MessageConsumer(channel, cipher, processor, component, TestUtils.createStep())
    }


    def "should decrypt and process message successfully"() {

        when:
        consumer.handleDelivery(consumerTag, envelope, amqpProperties, encryptedMessage.getBytes());

        then:
        1 * processor.processMessage({
            assert JSON.stringify(it.getMessage().getBody()) == '{"content":"Hello world!"}'
            assert it.step != null
            it.amqpProperties == amqpProperties
        }, component) >> new ExecutionStats(1, 0, 0)
        1 * channel.basicAck(123456, true)
        0 * _

    }


    def "should reject message if error callback has count > 0"() {

        when:
        consumer.handleDelivery(consumerTag, envelope, amqpProperties, encryptedMessage.getBytes());

        then:
        1 * processor.processMessage({
            assert JSON.stringify(it.getMessage().getBody()) == '{"content":"Hello world!"}'
            assert it.step != null
            it.amqpProperties == amqpProperties
        }, component) >> new ExecutionStats(0, 1, 0)
        1 * channel.basicReject(123456, false)
        0 * _

    }

    def "should reject message if processing fails"() {

        when:
        consumer.handleDelivery(consumerTag, envelope, amqpProperties, encryptedMessage.getBytes());

        then:
        1 * processor.processMessage({
            assert JSON.stringify(it.getMessage().getBody()) == '{"content":"Hello world!"}'
            assert it.step != null
            it.amqpProperties == amqpProperties
        }, component) >> { throw new Exception("Ouch") }
        1 * channel.basicReject(123456, false)

    }

    def "should reject message if decryption fails"() {
        when:
        consumer.handleDelivery(consumerTag, envelope, amqpProperties, "here be monsters".getBytes());

        then:
        1 * channel.basicReject(123456, false)

    }

}
