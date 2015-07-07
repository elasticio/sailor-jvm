package groovy.io.elastic.sailor

import com.rabbitmq.client.AMQP
import io.elastic.sailor.MessageConsumer
import io.elastic.sailor.Sailor
import spock.lang.Specification

class MessageConsumerSpec extends Specification {

    def "should decrypt message and pass parameters to callback"() {
        setup:
        def callback = Mock(Sailor.Callback)
        def consumer = new MessageConsumer(null, null, callback);
        def consumerTag = "tag12345";

        def headers = new HashMap();
        headers.put("execId", "exec1");
        headers.put("taskId", "task2");
        headers.put("userId", "user3");

        def options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .build();
        when:
        byte[] messageContent = new String("{\"content\":\"Hello world!\"}").getBytes();
        consumer.handleDelivery(consumerTag, null, options, messageContent);
        then:
        1 * callback.receive({it.toString() == "{\"body\":{\"content\":\"Hello world!\"},\"attachments\":{}}"}, headers, consumerTag)
    }

    def "should decrypt encrypted message and pass parameters to callback"() {
        setup:
        def callback = Mock(Sailor.Callback)
        def consumer = new MessageConsumer(null, "testCryptoPassword", callback);
        def consumerTag = "tag12345";

        def headers = new HashMap();
        headers.put("execId", "exec1");
        headers.put("taskId", "task2")
        headers.put("userId", "user3");

        def options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .build();
        when:

        byte[] messageContent = new String("MhcbHNshDRy6RNubmFJ+u4tcKKTKT6H50uYMyBXhws1xjvVKRtEC0hEg0/R2Zecy").getBytes();
        consumer.handleDelivery(consumerTag, null, options, messageContent);
        then:
        1 * callback.receive({it.toString() == "{\"someKey\":\"someValue\"}"}, headers, consumerTag)
    }


}