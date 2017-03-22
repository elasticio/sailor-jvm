package io.elastic.sailor

import io.elastic.sailor.component.SimpleSelectModelProvider
import org.apache.commons.io.IOUtils
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class ServiceIntegrationSpec extends Specification  {


    @Shared
    def blockingVar

    def setupSpec() {
        blockingVar = new BlockingVariable(5)
        System.setProperty(Constants.ENV_VAR_POST_RESULT_URL, 'http://admin:secret@localhost:8183')
        System.setProperty(Constants.ENV_VAR_CFG, '{}')

        Server server = new Server(8183);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {

                ServiceIntegrationSpec.this.blockingVar.set(
                        [contentType:baseRequest.getHeader("Content-type"),
                         body:IOUtils.toString(baseRequest.getInputStream())])

                response.setHeader("Content-type", 'application/json')
                response.getOutputStream().write("OK".getBytes());
                response.getOutputStream().close();
            }
        });

        server.start()
    }

    def "run verifyCredentials successfully"() {
        when:
        Service.createServiceInstanceAndExecute(ServiceMethods.verifyCredentials.name())

        then:
        def result = blockingVar.get()
        result.contentType == 'application/json'
        result.body == '{"status":"success","data":{"verified":true}}'
    }

    def "run selectModel successfully"() {
        setup:
        System.setProperty(Constants.ENV_VAR_GET_MODEL_METHOD, SimpleSelectModelProvider.class.getName())

        when:
        Service.createServiceInstanceAndExecute(ServiceMethods.selectModel.name())

        then:
        def result = blockingVar.get()
        result.contentType == 'application/json'
        result.body == '{"status":"success","data":{"de":"Germany","us":"United States","cfg":{}}}'
    }

    def "run getMetaModel successfully"() {
        setup:
        System.setProperty(Constants.ENV_VAR_ACTION_OR_TRIGGER, "helloworldaction")

        when:
        Service.createServiceInstanceAndExecute(ServiceMethods.getMetaModel.name())

        then:
        def result = blockingVar.get()
        result.contentType == 'application/json'
        result.body == '{"status":"success","data":{"in":{"type":"object"},"out":{}}}'
    }
}
