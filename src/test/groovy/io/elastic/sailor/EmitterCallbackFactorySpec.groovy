package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.api.Message
import io.elastic.sailor.impl.*
import spock.lang.Shared

class EmitterCallbackFactorySpec extends ApiAwareSpecification {


    @Shared
    EmitterCallbackFactory factory;

    ExecutionContext ctx = new ExecutionContext(
            TestUtils.createStep(), new byte[0], new Message.Builder().build(), Utils.buildAmqpProperties([:]), new ContainerContext());

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule(), new AmqpAwareModule());

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

    def "should create updateKeys callback"() {
        when:
        def callback = factory.createUpdateKeysCallback(ctx)
        then:
        callback != null
        callback instanceof UpdateKeysCallback
    }
}
