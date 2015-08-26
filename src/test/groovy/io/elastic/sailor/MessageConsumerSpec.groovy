package io.elastic.sailor

import com.google.gson.JsonObject
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Envelope
import io.elastic.api.Message
import spock.lang.Shared
import spock.lang.Specification

class MessageConsumerSpec extends Specification {

    Channel channel = Mock()
    MessageProcessor processor = Mock()

    @Shared
    CipherWrapper cipher = new CipherWrapper("testCryptoPassword", "iv=any16_symbols")

    def consumer

    @Shared
    def options

    @Shared
    def headers

    def consumerTag = "tag12345"

    def envelope = new Envelope(123456, false, "test", "test2")

    @Shared
    def encryptedMessage

    def setupSpec() {

        headers = ["execId": "exec1", "taskId": "task2", "userId": "user3"] as Map

        options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .build();

        def body = new JsonObject();
        body.addProperty("content", "Hello world!");

        def msg = new Message.Builder().body(body).build();
        encryptedMessage = cipher.encryptMessage(msg)
    }

    def setup() {
        consumer = new MessageConsumer(channel, cipher, processor);
    }

    def "should decrypt and process message successfully"() {

        when:
        consumer.handleDelivery(consumerTag, envelope, options, encryptedMessage.getBytes());

        then:
        1 * processor.processMessage({
            it.getBody().toString() == "{\"content\":\"Hello world!\"}"
        }, headers, 123456)
        0 * _

    }

    def "should reject message if processing fails"() {

        when:
        consumer.handleDelivery(consumerTag, envelope, options, encryptedMessage.getBytes());

        then:
        1 * processor.processMessage({
            it.getBody().toString() == "{\"content\":\"Hello world!\"}"
        }, headers, 123456) >> { throw new Exception("Ouch") }
        1 * channel.basicReject(123456, false)

    }

    def "should reject message if decryption fails"() {
        when:
        consumer.handleDelivery(consumerTag, envelope, options, "here be monsters".getBytes());

        then:
        1 * channel.basicReject(123456, false)

    }

}
