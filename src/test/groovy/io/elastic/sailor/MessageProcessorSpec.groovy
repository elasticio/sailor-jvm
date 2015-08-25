package io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.api.Message
import spock.lang.Specification

class MessageProcessorSpec  extends Specification {

    def envVars = [
            (ServiceSettings.ENV_VAR_TASK) : "{\"_id\":\"5559edd38968ec0736000003\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}",
            (ServiceSettings.ENV_VAR_STEP_ID): "step_1",
            (ServiceSettings.ENV_VAR_AMQP_URI) : "amqp://guest:guest@some-rabbit-server.com:5672",
            (ServiceSettings.ENV_VAR_LISTEN_MESSAGES_ON) : "5559edd38968ec0736000003:test_exec:step_1:messages",
            (ServiceSettings.ENV_VAR_PUBLISH_MESSAGES_TO) : "5527f0ea43238e5d5f000002_exchange",
            (ServiceSettings.ENV_VAR_DATA_ROUTING_KEY) : "5559edd38968ec0736000003.test_exec.step_1.message",
            (ServiceSettings.ENV_VAR_ERROR_ROUTING_KEY) : "5559edd38968ec0736000003.test_exec.step_1.error",
            (ServiceSettings.ENV_VAR_SNAPSHOT_ROUTING_KEY) : "5559edd38968ec0736000003.test_exec.step_1.snapshot",
            (ServiceSettings.ENV_VAR_REBOUND_ROUTING_KEY) :  "5559edd38968ec0736000003.test_exec.step_1.rebound",
            (ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_PASSWORD) : "crypt123456",
            (ServiceSettings.ENV_VAR_MESSAGE_CRYPTO_IV) : "0000000000000000",
            (ServiceSettings.ENV_VAR_COMPONENT_PATH) : "/spec/component/"
    ]

    def setup() {
        envVars.each { key, value ->
            println "Setting env var ${key} to ${value}"
            System.setProperty(key, value);
        };
    }

    def cleanup() {
        envVars.each { key, value ->
            println "Removing value of env var ${key}"
            System.getProperties().remove(key);
        }
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

    long getDeliveryTag(){
        12345
    }

    def makeProcessor(AMQPWrapperInterface amqp){
        return new MessageProcessor(
            new ExecutionDetails(),
            getIncomingMessage(),
            getIncomingMessageHeaders(),
            getDeliveryTag(),
            amqp,
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
