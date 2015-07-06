package groovy.io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.api.EventEmitter
import io.elastic.api.ExecutionParameters
import io.elastic.api.Message
import io.elastic.api.EventEmitter.Callback

import io.elastic.sailor.TaskExecutor
import spock.lang.Specification

class TaskExecutorSpec extends Specification {

    def "should throw exception if class is not found"() {
        when:
            new TaskExecutor("some.unknown.class");
        then:
            RuntimeException e = thrown()
            e.getMessage() == "Class some.unknown.class is not found"
    }

    def "should throw exception if provided class is not a Component"() {
        when:
            new TaskExecutor("com.google.gson.JsonObject");
        then:
            RuntimeException e = thrown()
            e.getMessage() == "Class com.google.gson.JsonObject is not io.elastic.api.Component"
    }

    def "should create TaskExecutor for valid component without errors"() {
        when:
            new TaskExecutor("groovy.io.elastic.sailor.component.TestAction");
        then:
            notThrown(RuntimeException)
    }

    def "should execute TestAction and emit all necessary events"() {

        def content = new JsonObject();
        content.addProperty("someProperty", "someValue");
        def message = new Message.Builder().body(content).build();
        def params = new ExecutionParameters.Builder(message).build();

        def errorCallback = Mock(Callback)
        def snapshotCallback = Mock(Callback)
        def dataCallback = Mock(Callback)
        def reboundCallback = Mock(Callback)

        when:
            def executor = new TaskExecutor("groovy.io.elastic.sailor.component.TestAction");
            executor.onData(dataCallback).onSnapshot(snapshotCallback).onError(errorCallback).onRebound(reboundCallback);
            executor.execute(params);
        then:
            1 * dataCallback.receive({it.toString() == '{"body":{"someProperty":"someValue"},"attachments":{}}'})
            1 * snapshotCallback.receive({it.toString() == '{"lastUpdate":"2015-07-04"}'})
            1 * reboundCallback.receive({it.toString() == 'Please retry later'})
            1 * errorCallback.receive({it.getMessage() == "Error happened in TestAction!"})
    }

    def "should execute SleepAction and emit error on timeout"() {

        def content = new JsonObject();
        content.addProperty("someProperty", "someValue");
        def message = new Message.Builder().body(content).build();
        def params = new ExecutionParameters.Builder(message).build();

        def errorCallback = Mock(Callback)
        def snapshotCallback = Mock(Callback)
        def dataCallback = Mock(Callback)
        def reboundCallback = Mock(Callback)

        when:
            def executor = new TaskExecutor("groovy.io.elastic.sailor.component.SleepAction");
            executor.onData(dataCallback).onSnapshot(snapshotCallback).onError(errorCallback).onRebound(reboundCallback);
            executor.setTimeout(200);
            executor.execute(params);
        then:
            1 * errorCallback.receive({it.getMessage() == "Processing time out - groovy.io.elastic.sailor.component.SleepAction"})
            0 * dataCallback.receive({})
    }
}

