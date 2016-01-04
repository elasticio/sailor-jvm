package io.elastic.sailor

import com.google.gson.JsonParser
import io.elastic.api.Message
import spock.lang.Specification

class ExecutionContextSpec extends Specification {


    def "should create data callback"() {
        given:
        def task = new JsonParser().parse("{\"_id\":\"5559edd38968ec0736000003\",\"userId\":\"010101\",\"data\":{\"step_1\":{\"uri\":\"546456456456456\"}},\"recipe\":{\"nodes\":[{\"id\":\"step_1\",\"compId\":\"testcomponent\",\"function\":\"test\"}]}}")
        def originalHeaders = [
                execId: "_exec_01",
                taskId: "5559edd38968ec0736000003",
                userId: "010101"
        ] as Map

        ExecutionContext ctx = new ExecutionContext(
                "step_1", task, new Message.Builder().build(), originalHeaders);

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
        headers.elasticio_feature_flag_skip_message_url_decoding == '1'
    }
}
