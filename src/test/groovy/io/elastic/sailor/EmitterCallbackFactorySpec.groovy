package io.elastic.sailor

import com.google.gson.JsonObject
import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.api.Message
import spock.lang.Shared
import spock.lang.Specification

class EmitterCallbackFactorySpec extends Specification {


    @Shared
    EmitterCallbackFactory factory;
    ExecutionContext ctx = new ExecutionContext(
            "step_1", new JsonObject(), new Message.Builder().build(), Collections.emptyMap());

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        factory = injector.getInstance(EmitterCallbackFactory.class);
    }

    def "should create data callback"() {
        when:
        def callback = factory.createDataCallback(ctx)
        then:
        callback != null
        callback instanceof DataCallback
    }

    def "should create error callback"() {
        when:
        def callback = factory.createErrorCallback(ctx)
        then:
        callback != null
        callback instanceof ErrorCallback
    }

    def "should create snapshot callback"() {
        when:
        def callback = factory.createSnapshotCallback(ctx)
        then:
        callback != null
        callback instanceof SnapshotCallback
    }

    def "should create rebound callback"() {
        when:
        def callback = factory.createReboundCallback(ctx)
        then:
        callback != null
        callback instanceof ReboundCallback
    }
}
