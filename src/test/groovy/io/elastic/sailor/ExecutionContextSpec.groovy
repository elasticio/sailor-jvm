package io.elastic.sailor

import io.elastic.api.Message
import spock.lang.Specification

class ExecutionContextSpec extends Specification {


    def "should build default headers properly"() {
        given:
        def originalHeaders = [
                execId: "_exec_01",
                taskId: "5559edd38968ec0736000003",
                userId: "010101"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        print headers
        headers.compId == 'testcomponent'
        headers.function == 'test'
        headers.stepId == 'step_1'
        headers.start != null
        headers.taskId == '5559edd38968ec0736000003'
        headers.userId == '010101'
        headers.execId == '_exec_01'
    }

    def "should build default headers with reply_to"() {
        given:
        def originalHeaders = [
                execId: "_exec_01",
                taskId: "5559edd38968ec0736000003",
                userId: "010101",
                reply_to: "_reply_to_this_queue"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                TestUtils.createStep(),
                new Message.Builder().build(),
                Utils.buildAmqpProperties(originalHeaders));

        when:
        def headers = ctx.buildDefaultHeaders()

        then:
        print headers
        headers.compId == 'testcomponent'
        headers.function == 'test'
        headers.stepId == 'step_1'
        headers.start != null
        headers.taskId == '5559edd38968ec0736000003'
        headers.userId == '010101'
        headers.execId == '_exec_01'
        headers.reply_to == '_reply_to_this_queue'
    }

    def "should build default headers with x-eio-meta- headers"() {
        given:
        def originalHeaders = [
                execId: "_exec_01",
                taskId: "5559edd38968ec0736000003",
                userId: "010101",
                reply_to: "_reply_to_this_queue",
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
        print headers
        headers.compId == 'testcomponent'
        headers.function == 'test'
        headers.stepId == 'step_1'
        headers.start != null
        headers.taskId == '5559edd38968ec0736000003'
        headers.userId == '010101'
        headers.execId == '_exec_01'
        headers.reply_to == '_reply_to_this_queue'
        headers[Constants.AMQP_META_HEADER_PREFIX + "lowercase"] == 'I am lowercase'
        headers[Constants.AMQP_META_HEADER_PREFIX + "mixedcase"] == 'Eventually to become lowercase'
    }
}
