package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import spock.lang.Shared
import spock.lang.Specification

class MessageProcessorSpec extends Specification {


    @Shared
    MessageProcessor processor

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new TestModule());

        processor = injector.getInstance(MessageProcessor.class);
    }

    def "should create instance"() {
        expect: processor != null
    }
}
