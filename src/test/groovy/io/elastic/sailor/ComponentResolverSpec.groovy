package io.elastic.sailor

import com.google.inject.Guice
import com.google.inject.Injector
import io.elastic.sailor.component.CredentialsVerifierImpl
import spock.lang.Shared
import spock.lang.Specification

class ComponentResolverSpec extends Specification {

    @Shared
    def resolver;

    def setupSpec() {
        Injector injector = Guice.createInjector(new SailorModule(), new SailorTestModule());

        resolver = injector.getInstance(ComponentDescriptorResolver.class);
    }

    def "should successfully load component.json if no slash at the end"() {
        when:
        new ComponentDescriptorResolver()
        then:
        notThrown(RuntimeException)
    }

    def "should find trigger"() {
        when:
        def resolver = new ComponentDescriptorResolver()
        def result = resolver.findModule("sleep")
        then:
        notThrown(RuntimeException)
        result == "io.elastic.sailor.component.SleepAction"
    }

    def "should find action"() {
        when:
        def result = resolver.findModule("helloworldaction")
        then:
        notThrown(RuntimeException)
        result == "io.elastic.sailor.component.HelloWorldAction"
    }

    def "should throw exception if trigger or action is not found"() {
        when:
        resolver.findModule("missing_action")
        then:
        RuntimeException e = thrown()
        e.getMessage() == "'missing_action' trigger or action is not found"
    }

    def "should find credentials verifier"() {
        when:
        def result = resolver.findCredentialsVerifier();
        then:
        result == CredentialsVerifierImpl.class.getName()
    }


}
