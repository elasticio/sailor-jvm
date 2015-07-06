package groovy.io.elastic.sailor

import spock.lang.Specification
import io.elastic.sailor.ComponentResolver

class ComponentResolverSpec extends Specification {

    def "should throw error if component.json is not found"() {
        when:
            new ComponentResolver("src/test/java/groovy/io")
        then:
            RuntimeException e = thrown()
            e.getMessage() == "component.json is not found in /home/lena/elasticio/test/sailor-jvm/src/test/java/groovy/io"
    }

    def "should successfully load component.json"() {
        when:
            new ComponentResolver("/src/test/java/groovy/io/elastic/sailor/component/")
        then:
            notThrown(RuntimeException)
    }

    def "should successfully load component.json if no slash at the beginning"() {
        when:
            new ComponentResolver("src/test/java/groovy/io/elastic/sailor/component/")
        then:
            notThrown(RuntimeException)
    }

    def "should successfully load component.json if no slash at the end"() {
        when:
            new ComponentResolver("src/test/java/groovy/io/elastic/sailor/component")
        then:
            notThrown(RuntimeException)
    }

    def "should find trigger"() {
        when:
            def resolver = new ComponentResolver("src/test/java/groovy/io/elastic/sailor/component")
            def result = resolver.findTriggerOrAction("query_price_lists")
        then:
            notThrown(RuntimeException)
            result == "io.elastic.sap.bydesign.triggers.pricelist.QueryPriceLists"
    }

    def "should find action"() {
        when:
            def resolver = new ComponentResolver("src/test/java/groovy/io/elastic/sailor/component")
            def result = resolver.findTriggerOrAction("create_customer")
        then:
            notThrown(RuntimeException)
            result == "io.elastic.sap.bydesign.actions.customer.CreateCustomer"
    }

    def "should throw exception if trigger or action is not found"() {
        when:
            def resolver = new ComponentResolver("src/test/java/groovy/io/elastic/sailor/component")
            resolver.findTriggerOrAction("missing_action")
        then:
            RuntimeException e = thrown()
            e.getMessage() == "missing_action is not found"
    }




}
