package io.elastic.sailor

import com.google.gson.JsonObject
import io.elastic.sailor.component.SimpleMetadataProvider
import io.elastic.sailor.component.SimpleSelectModelProvider
import spock.lang.Specification

class ServiceMethodsSpec extends Specification {

    def "select model"() {
        setup:
        def cfg = new JsonObject()
        cfg.addProperty("apiKey", "secret")

        def params = new ServiceExecutionParameters.Builder()
                .className(SimpleSelectModelProvider.class.getName())
                .configuration(cfg)
                .modelClassName(SimpleSelectModelProvider.class.getName())
                .build();

        when:
        def result = ServiceMethods.selectModel.execute(params)

        then:
        result.toString() == '{"de":"Germany","us":"United States","cfg":{"apiKey":"secret"}}'
    }

    def "meta model"() {
        setup:
        def cfg = new JsonObject()
        cfg.addProperty("apiKey", "secret")

        def triggerOrAction = new JsonObject()
        triggerOrAction.addProperty("dynamicMetadata", SimpleMetadataProvider.class.getName())

        def params = new ServiceExecutionParameters.Builder()
                .className(SimpleMetadataProvider.class.getName())
                .configuration(cfg)
                .triggerOrAction(triggerOrAction)
                .build();

        when:
        def result = ServiceMethods.getMetaModel.execute(params)

        then:
        result.toString() == '{"in":{"type":"object"},"out":{}}'
    }
}
