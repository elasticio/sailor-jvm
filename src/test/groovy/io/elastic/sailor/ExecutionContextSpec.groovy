package io.elastic.sailor

import io.elastic.api.Message
import spock.lang.Specification

class ExecutionContextSpec extends Specification {


    def "should build default headers properly"() {
        given:
        def originalHeaders = [
                (ExecutionContext.HEADER_EXEC_ID): "_exec_01",
                (ExecutionContext.HEADER_TASK_ID): "5559edd38968ec0736000003",
                (ExecutionContext.HEADER_USER_ID): "010101"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[ExecutionContext.HEADER_COMPONENT_ID] == 'testcomponent'
        headers[ExecutionContext.HEADER_FUNCTION] == 'test'
        headers[ExecutionContext.HEADER_STEP_ID] == 'step_1'
        headers[ExecutionContext.HEADER_START_TIMESTAMP] != null
        headers[ExecutionContext.HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[ExecutionContext.HEADER_USER_ID] == '010101'
        headers[ExecutionContext.HEADER_EXEC_ID] == '_exec_01'
    }

    def "should build default headers with reply_to"() {
        given:
        def originalHeaders = [
                (ExecutionContext.HEADER_EXEC_ID): "_exec_01",
                (ExecutionContext.HEADER_TASK_ID): "5559edd38968ec0736000003",
                (ExecutionContext.HEADER_USER_ID): "010101",
                (ExecutionContext.HEADER_REPLY_TO): "_reply_to_this_queue"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[ExecutionContext.HEADER_COMPONENT_ID] == 'testcomponent'
        headers[ExecutionContext.HEADER_FUNCTION] == 'test'
        headers[ExecutionContext.HEADER_STEP_ID] == 'step_1'
        headers[ExecutionContext.HEADER_START_TIMESTAMP] != null
        headers[ExecutionContext.HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[ExecutionContext.HEADER_USER_ID] == '010101'
        headers[ExecutionContext.HEADER_EXEC_ID] == '_exec_01'
        headers[ExecutionContext.HEADER_REPLY_TO] == '_reply_to_this_queue'
    }

    def "should build default headers with x-eio-meta- headers"() {
        given:
        def originalHeaders = [
                (ExecutionContext.HEADER_EXEC_ID): "_exec_01",
                (ExecutionContext.HEADER_TASK_ID): "5559edd38968ec0736000003",
                (ExecutionContext.HEADER_USER_ID): "010101",
                (ExecutionContext.HEADER_REPLY_TO): "_reply_to_this_queue",
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
        headers[ExecutionContext.HEADER_COMPONENT_ID] == 'testcomponent'
        headers[ExecutionContext.HEADER_FUNCTION] == 'test'
        headers[ExecutionContext.HEADER_STEP_ID] == 'step_1'
        headers[ExecutionContext.HEADER_START_TIMESTAMP] != null
        headers[ExecutionContext.HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[ExecutionContext.HEADER_USER_ID] == '010101'
        headers[ExecutionContext.HEADER_EXEC_ID] == '_exec_01'
        headers[ExecutionContext.HEADER_REPLY_TO] == '_reply_to_this_queue'
        headers[Constants.AMQP_META_HEADER_PREFIX + "lowercase"] == 'I am lowercase'
        headers[Constants.AMQP_META_HEADER_PREFIX + "mixedcase"] == 'Eventually to become lowercase'
    }


    def "should build with parent message id"() {
        given:
        def msg = new Message.Builder().build();
        def originalHeaders = [
                (ExecutionContext.HEADER_EXEC_ID): "_exec_01",
                (ExecutionContext.HEADER_TASK_ID): "5559edd38968ec0736000003",
                (ExecutionContext.HEADER_USER_ID): "010101",
                (ExecutionContext.HEADER_PARENT_MESSAGE_ID): msg.id.toString()
        ] as Map
        def props = Utils.buildAmqpProperties(originalHeaders)

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                msg,
                props);

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        headers[ExecutionContext.HEADER_COMPONENT_ID] == 'testcomponent'
        headers[ExecutionContext.HEADER_FUNCTION] == 'test'
        headers[ExecutionContext.HEADER_STEP_ID] == 'step_1'
        headers[ExecutionContext.HEADER_START_TIMESTAMP] != null
        headers[ExecutionContext.HEADER_TASK_ID] == '5559edd38968ec0736000003'
        headers[ExecutionContext.HEADER_USER_ID] == '010101'
        headers[ExecutionContext.HEADER_EXEC_ID] == '_exec_01'
        headers[ExecutionContext.HEADER_PARENT_MESSAGE_ID] == props.messageId
    }
}
