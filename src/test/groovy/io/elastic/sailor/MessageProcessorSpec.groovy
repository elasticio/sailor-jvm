package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import spock.lang.Shared

class MessageProcessorSpec extends ApiAwareSpecification {


    @Shared
    MessageProcessor processor

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        processor = injector.getInstance(MessageProcessor.class);
    }

    def "should create instance"() {
        expect: processor != null
    }
}
