package io.elastic.sailor

import spock.lang.Specification

class ComponentResolverSpec extends Specification {

    def "should throw error if component.json is not found"() {
        when:
            new ComponentResolver("src/test/java/groovy/io")
        then:
            RuntimeException e = thrown()
            e.getMessage().contains("component.json is not found")
    }

    def "should successfully load component.json"() {
        when:
            new ComponentResolver("/src/test/java/io/elastic/sailor/component/")
        then:
            notThrown(RuntimeException)
    }

    def "should successfully load component.json if no slash at the beginning"() {
        when:
            new ComponentResolver("src/test/java/io/elastic/sailor/component/")
        then:
            notThrown(RuntimeException)
    }

    def "should successfully load component.json if no slash at the end"() {
        when:
            new ComponentResolver("src/test/java/io/elastic/sailor/component")
        then:
            notThrown(RuntimeException)
    }

    def "should find trigger"() {
        when:
            def resolver = new ComponentResolver("src/test/java/io/elastic/sailor/component")
            def result = resolver.findTriggerOrAction("sleep")
        then:
            notThrown(RuntimeException)
            result == "io.elastic.sailor.component.SleepAction"
    }

    def "should find action"() {
        when:
            def resolver = new ComponentResolver("src/test/java/io/elastic/sailor/component")
            def result = resolver.findTriggerOrAction("test")
        then:
            notThrown(RuntimeException)
            result == "io.elastic.sailor.component.TestAction"
    }

    def "should throw exception if trigger or action is not found"() {
        when:
            def resolver = new ComponentResolver("src/test/java/io/elastic/sailor/component")
            resolver.findTriggerOrAction("missing_action")
        then:
            RuntimeException e = thrown()
            e.getMessage() == "'missing_action' trigger or action is not found"
    }




}
