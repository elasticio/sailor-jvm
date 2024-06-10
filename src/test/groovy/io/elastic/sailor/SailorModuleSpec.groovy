package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.Key
import com.google.inject.name.Names

class SailorModuleSpec extends ApiAwareSpecification {

    Injector injector;

    def setup() {
       injector = Guice.createInjector(new SailorModule(), new SailorTestModule())
    }



    def "it should provide step JSON"() {
        when:
        def step = injector.getInstance(Key.get(Step.class, Names.named(Constants.NAME_STEP_JSON)));

        then:
        step.id == 'step_1'
        step.compId == 'testcomponent'
        step.function == 'test'
        step.cfg.toString() == '{"apiKey":"secret"}'
    }
}
