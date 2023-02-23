package io.elastic.sailor

import io.elastic.sailor.component.CredentialsVerifierImpl
import io.elastic.sailor.component.SimpleMetadataProvider
import io.elastic.sailor.component.SimpleSelectModelProvider
import spock.lang.Specification

import jakarta.json.Json

class ServiceMethodsSpec extends Specification {

    def "select model"() {
        setup:
        def cfg = Json.createObjectBuilder()
                .add("apiKey", "secret")
                .build()

        def params = new ServiceExecutionParameters.Builder()
                .className(SimpleSelectModelProvider.class.getName())
                .configuration(cfg)
                .modelClassName(SimpleSelectModelProvider.class.getName())
                .build()

        when:
        def result = ServiceMethods.selectModel.execute(params)

        then:
        result.toString() == '{"de":"Germany","us":"United States","cfg":{"apiKey":"secret"}}'
    }

    def "meta model"() {
        setup:
        def cfg = Json.createObjectBuilder()
                .add("apiKey", "secret")
                .build()

        def triggerOrAction = Json.createObjectBuilder()
                .add("dynamicMetadata", SimpleMetadataProvider.class.getName())
                .build()

        def params = new ServiceExecutionParameters.Builder()
                .className(SimpleMetadataProvider.class.getName())
                .configuration(cfg)
                .triggerOrAction(triggerOrAction)
                .build()

        when:
        def result = ServiceMethods.getMetaModel.execute(params)

        then:
        result.toString() == '{"in":{"type":"object"},"out":{}}'
    }

    def "verify credentials when verified is not set"() {
        setup:
        def cfg = Json.createObjectBuilder()
                .add("apiKey", "secret")
                .build()

        def params = new ServiceExecutionParameters.Builder()
                .configuration(cfg)
                .build()

        when:
        def result = ServiceMethods.verifyCredentials.execute(params)

        then:
        result.toString() == '{"verified":true}'
    }

    def "verify credentials successfully with given verifier"() {
        setup:
        def cfg = Json.createObjectBuilder()
                .add("apiKey", "secret")
                .build()

        def params = new ServiceExecutionParameters.Builder()
                .configuration(cfg)
                .credentialsVerifierClassName(CredentialsVerifierImpl.class.getName())
                .build()

        when:
        def result = ServiceMethods.verifyCredentials.execute(params)

        then:
        result.toString() == '{"verified":true}'
    }

    def "verify credentials with error with given verifier"() {
        setup:
        def cfg = Json.createObjectBuilder()
                .add("apiKey", "secret")
                .add(CredentialsVerifierImpl.VERIFICATION_RESULT_CFG_KEY, false)
                .build()
        print(cfg)

        def params = new ServiceExecutionParameters.Builder()
                .configuration(cfg)
                .credentialsVerifierClassName(CredentialsVerifierImpl.class.getName())
                .build()

        when:
        def result = ServiceMethods.verifyCredentials.execute(params)

        then:
        result.toString() == '{"verified":false,"reason":"Invalid credentials"}'
    }
}
