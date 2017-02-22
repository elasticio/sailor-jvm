package io.elastic.sailor

import io.elastic.api.Message
import spock.lang.Specification

class ExecutionContextSpec extends Specification {


    def "should build default headers properly"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
    }

    def "should build default headers with reply_to"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_REPLY_TO): "_reply_to_this_queue"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        headers[Constants.AMQP_HEADER_REPLY_TO] == '_reply_to_this_queue'
    }

    def "should build default headers with x-eio-meta- headers"() {
        given:
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_REPLY_TO): "_reply_to_this_queue",
                (Constants.AMQP_META_HEADER_PREFIX + "lowercase"): "I am lowercase",
                "X-eio-meta-miXeDcAse": "Eventually to become lowercase"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        headers[Constants.AMQP_HEADER_REPLY_TO] == '_reply_to_this_queue'
        headers[Constants.AMQP_META_HEADER_PREFIX + "lowercase"] == 'I am lowercase'
        headers[Constants.AMQP_META_HEADER_PREFIX + "mixedcase"] == 'Eventually to become lowercase'
    }


    def "should build with parent message id"() {
        given:
        def msg = new Message.Builder().build();
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_PARENT_MESSAGE_ID): msg.id.toString()
        ] as Map
        def props = Utils.buildAmqpProperties(originalHeaders)

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                msg,
                props);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        headers[Constants.AMQP_HEADER_PARENT_MESSAGE_ID] == props.messageId
    }

    def "should build amqp properties successfully"() {
        given:
        def msg = new Message.Builder().build();
        def originalHeaders = [
                (Constants.AMQP_HEADER_EXEC_ID): "_exec_01",
                (Constants.AMQP_HEADER_TASK_ID): "5559edd38968ec0736000003",
                (Constants.AMQP_HEADER_USER_ID): "010101",
                (Constants.AMQP_HEADER_PARENT_MESSAGE_ID): msg.id.toString()
        ] as Map
        def props = Utils.buildAmqpProperties(originalHeaders)

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                msg,
                props);
        def uuid = UUID.randomUUID()

        when:
        def properties = ctx.buildAmqpProperties(uuid)

        then: "Should have all AMQP properties"
        properties.contentType == "application/json"
        properties.contentEncoding == "utf8"
        properties.deliveryMode == 2
        properties.priority == 1

        then: "Should have all headers"
        properties.headers[Constants.AMQP_HEADER_COMPONENT_ID] == 'testcomponent'
        properties.headers[Constants.AMQP_HEADER_FUNCTION] == 'test'
        properties.headers[Constants.AMQP_HEADER_STEP_ID] == 'step_1'
        properties.headers[Constants.AMQP_HEADER_START_TIMESTAMP] != null
        properties.headers[Constants.AMQP_HEADER_TASK_ID] == '5559edd38968ec0736000003'
        properties.headers[Constants.AMQP_HEADER_USER_ID] == '010101'
        properties.headers[Constants.AMQP_HEADER_EXEC_ID] == '_exec_01'
        properties.headers[Constants.AMQP_HEADER_PARENT_MESSAGE_ID] == props.messageId
        properties.headers[Constants.AMQP_HEADER_MESSAGE_ID] == uuid.toString()
    }
}
