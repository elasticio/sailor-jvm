package groovy.io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.api.Message
import io.elastic.sailor.AMQPWrapperInterface
import io.elastic.sailor.CipherWrapper
import io.elastic.sailor.MessageProcessor
import io.elastic.sailor.Settings
import spock.lang.Specification
import io.elastic.api.Message

class MessageProcessorSpec  extends Specification {

    def getSettings(){
        def envVars  = new HashMap<String, String>();
        envVars.put("TASK", "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}");
        envVars.put("STEP_ID", "step_1");
        envVars.put("AMQP_URI", "amqp://guest:guest@some-rabbit-server.com:5672");
        envVars.put("LISTEN_MESSAGES_ON", "5559edd38968ec0736000003:test_exec:step_1:messages");
        envVars.put("PUBLISH_MESSAGES_TO", "5527f0ea43238e5d5f000002_exchange");
        envVars.put("DATA_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.message");
        envVars.put("ERROR_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.error");
        envVars.put("SNAPSHOT_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.snapshot");
        envVars.put("REBOUND_ROUTING_KEY", "5559edd38968ec0736000003.test_exec.step_1.rebound");
        envVars.put("MESSAGE_CRYPTO_PASSWORD", "crypt123456");
        envVars.put("COMPONENT_PATH", "/spec/component/");
        return new Settings(envVars);
    }

    def getIncomingMessage(){
        def body = new JsonObject()
        body.addProperty("incomingProperty1", "incomingValue1")
        body.addProperty("incomingProperty2", "incomingValue2")

        def attachments = new JsonObject()
        attachments.addProperty("incomingAttachment1", "incomingAttachment1Content")
        attachments.addProperty("incomingAttachment2", "incomingAttachment2Content")

        return new Message(body, attachments);
    }

    def getIncomingMessageHeaders(){
        def headers = new HashMap();
        headers.put("execId", "exec1");
        headers.put("taskId", "task2")
        headers.put("userId", "user3");
        return headers;
    }

    def getDeliveryTag(){
        12345
    }

    def makeProcessor(amqp){
        return new MessageProcessor(
            getIncomingMessage(),
            getIncomingMessageHeaders(),
            getDeliveryTag(),
            amqp,
            getSettings(),
            new CipherWrapper()
        );
    }

    def "should process data"() {
        given:
            def amqp = Mock(AMQPWrapperInterface)
            def processor = makeProcessor(amqp);

            def body = new JsonObject()
            body.addProperty("someProperty1", "someValue1")
            body.addProperty("someProperty2", "someValue2")

            def attachments = new JsonObject()
            attachments.addProperty("attachment1", "attachmentContent1")
            attachments.addProperty("attachment2", "attachmentContent2")

            def receivedMessage = new Message(body, attachments);
        when:
            processor.processData(receivedMessage);
        then:
            1 * amqp.sendData(_,_)
            // @TODO check content
    }

    def "should process error"() {
        given:
            def amqp = Mock(AMQPWrapperInterface)
            def processor = makeProcessor(amqp);
            def exception = new Exception("Something happened");
        when:
            processor.processError(exception);
        then:
            1 * amqp.sendError(_,_)
            // @TODO check content

    }

    def "should process snapshot"() {
        given:
            def amqp = Mock(AMQPWrapperInterface)
            def processor = makeProcessor(amqp);
            def snapshot = new JsonObject()
            snapshot.addProperty("somePropertyA", "someValueA")
            snapshot.addProperty("somePropertyB", "someValueB")
        when:
            processor.processSnapshot(snapshot);
        then:
            1 * amqp.sendSnapshot(_,_)
            // @TODO check content
    }

    def "should process rebound"() {
        given:
            def amqp = Mock(AMQPWrapperInterface)
            def processor = makeProcessor(amqp);
            def exception = new RuntimeException("Something happened");
        when:
            processor.processRebound(exception);
        then:
            1 * amqp.sendRebound(_,_)
            // @TODO check content
    }
}
