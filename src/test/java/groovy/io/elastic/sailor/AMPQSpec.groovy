package io.elastic.sailor
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import spock.lang.Specification

class AMPQSpec extends Specification {
    Map<String, String> envVars = Utils.validateSettings(new HashMap<String, String>() {{
        put("AMQP_URI", "amqp://test2/test2");
        put("TASK", "{'_id':'5559edd38968ec0736000003','data':{'step_1':{'account':'1234567890'}},'recipe':{'nodes':[{'id':'step_1','function':'list'}]}}");
        put("STEP_ID", "step_1");
        put("LISTEN_MESSAGES_ON", "5559edd38968ec0736000003:step_1:1432205514864:messages");
        put("PUBLISH_MESSAGES_TO", "userexchange:5527f0ea43238e5d5f000001");
        put("DATA_ROUTING_KEY", "5559edd38968ec0736000003:step_1:1432205514864:message");
        put("ERROR_ROUTING_KEY", "5559edd38968ec0736000003:step_1:1432205514864:error");
        put("REBOUND_ROUTING_KEY", "5559edd38968ec0736000003:step_1:1432205514864:rebound");
    }})

    JsonElement message = new JsonParser().parse("{" +
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
            "'content':'%7B%22content%22%3A%22Message%20content%22%7D'" +
        "}".replaceAll("'","\""));

    def "create, serviceUrl is missing" () {
        when:
        println("when")

        then:
        println("then")
    }
}
