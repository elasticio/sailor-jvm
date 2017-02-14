package io.elastic.sailor

import io.elastic.api.JSON
import io.elastic.api.Message
import junit.framework.Test
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
                TestUtils.createStep(), new Message.Builder().build(), originalHeaders);

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
                TestUtils.createStep(), new Message.Builder().build(), originalHeaders);

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
}
