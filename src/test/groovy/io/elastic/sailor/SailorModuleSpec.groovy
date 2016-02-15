package io.elastic.sailor

import com.google.gson.JsonObject
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
        def cfg = injector.getInstance(Key.get(JsonObject.class, Names.named(Constants.NAME_STEP_JSON)));

        then:
        cfg.toString() == '{"id":"step_1","compId":"testcomponent","function":"test"}'
    }
}
