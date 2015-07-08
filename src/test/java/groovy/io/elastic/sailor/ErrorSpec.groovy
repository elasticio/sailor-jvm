package groovy.io.elastic.sailor

import spock.lang.Specification
import io.elastic.sailor.Error

class ErrorSpec extends Specification {

    def "should build error from RuntimeException"() {
        when:
            def error = null;
            try {
                throw new RuntimeException("Exception message");
            } catch (Exception e) {
                error = new Error(e);
            }
        then:
            error.name == "Error"
            error.message == "Exception message"
    }


}
