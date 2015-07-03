package groovy.io.elastic.sailor
import com.google.gson.JsonParser
import io.elastic.sailor.AMQPWrapper
import io.elastic.sailor.CipherWrapper
import io.elastic.sailor.Utils
import spock.lang.Specification

class AMPQSpec extends Specification {
    def settings = Utils.validateSettings(new HashMap<String, String>() {{
        put("AMQP_URI", "amqp://test2/test2");
        put("TASK", "{'_id':'5559edd38968ec0736000003','data':{'step_1':{'account':'1234567890'}},'recipe':{'nodes':[{'id':'step_1','function':'list'}]}}");
        put("STEP_ID", "step_1");
        put("LISTEN_MESSAGES_ON", "5559edd38968ec0736000003:step_1:1432205514864:messages");
        put("PUBLISH_MESSAGES_TO", "userexchange:5527f0ea43238e5d5f000001");
        put("DATA_ROUTING_KEY", "5559edd38968ec0736000003:step_1:1432205514864:message");
        put("ERROR_ROUTING_KEY", "5559edd38968ec0736000003:step_1:1432205514864:error");
        put("REBOUND_ROUTING_KEY", "5559edd38968ec0736000003:step_1:1432205514864:rebound");
    }})

    def message = new JsonParser().parse("{" +
            "'fields':{" +
                "'consumerTag':'abcde'," +
                "'deliveryTag':12345," +
                "'exchange':'test'," +
                "'routingKey':'test.hello'" +
            "}," +
            "'properties':{" +
                "'contentType':'application/json'," +
                "'contentEncoding':'utf8'," +
                "'headers':{" +
                    "'taskId':'task1234567890'," +
                    "'execId':'exec1234567890'" +
                "}," +
                "'mandatory':true," +
                "'clusterId':''" +
            "}," +
            "'content':" + new CipherWrapper().encryptMessageContent(Utils.toJson("{\"content\":\"Message content\"}")) +
        "}".replaceAll("'","\""));

    def "Should send message to outgoing channel when process data" () {
        given:
            def amqp = new AMQPWrapper.ConnectionWrapper(settings)
        when:
        println("when")

        then:
        println("then")
    }
}
