package groovy.io.elastic.sailor

import com.google.gson.JsonObject
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Envelope
import io.elastic.sailor.CipherWrapper
import io.elastic.sailor.MessageConsumer
import io.elastic.sailor.Sailor
import spock.lang.Specification

class MessageConsumerSpec extends Specification {

    def "should decrypt message and pass parameters to callback"() {
        setup:
            def callback = Mock(Sailor.Callback)
            def cipher = new CipherWrapper();
            def consumer = new MessageConsumer(null, cipher, callback);
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

            def envelope = new Envelope(123456, false, "test", "test2");
        when:
            String message = new String("{\"body\":{\"content\":\"Hello world!\"}}");
            message = URLEncoder.encode(message, "UTF-8");
            consumer.handleDelivery(consumerTag, envelope, options, message.getBytes());
        then:
            1 * callback.receive(_, headers, 123456)
    }

    def "should decrypt encrypted message and pass parameters to callback"() {
        setup:
            def callback = Mock(Sailor.Callback)
            def cipher = new CipherWrapper();
            def consumer = new MessageConsumer(null, cipher, callback);
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
            def envelope = new Envelope(654321, false, "test", "test2");
        when:

            byte[] messageContent = new String("MhcbHNshDRy6RNubmFJ+u4tcKKTKT6H50uYMyBXhws1xjvVKRtEC0hEg0/R2Zecy").getBytes();
            consumer.handleDelivery(consumerTag, envelope, options, messageContent);
        then:
            1 * callback.receive({it.toString() == "{\"someKey\":\"someValue\"}"}, headers, consumerTag)
    }


}
