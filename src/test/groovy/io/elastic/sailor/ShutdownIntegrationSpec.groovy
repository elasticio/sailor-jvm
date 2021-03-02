package io.elastic.sailor

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.elastic.api.JSON
import io.elastic.sailor.impl.AmqpServiceImpl
import io.elastic.sailor.impl.CryptoServiceImpl
import io.elastic.sailor.impl.MessagePublisherImpl
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.concurrent.BlockingVariable

import javax.json.Json
import javax.json.JsonObject
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Stepwise
class ShutdownIntegrationSpec extends Specification {

    @Shared
    def amqp

    @Shared
    def cipher

    @Shared
    def prefix = 'sailor_jvm_shutdown_integration_test'

    @Shared
    def dataQueue = prefix + '_queue'

    @Shared
    def flowId = '5559edd38968ec0736000003'

    @Shared
    def stepCfg

    @Shared
    def sailor;

    @Shared
    def startupPayload

    @Shared
    def shutdownFlowId

    @Shared
    Server server

    @Shared
    Channel publishChannel

    def setupSpec() {

        System.setProperty(Constants.ENV_VAR_API_URI, 'http://localhost:8182')
        System.setProperty(Constants.ENV_VAR_API_USERNAME, 'test@test.com')
        System.setProperty(Constants.ENV_VAR_API_KEY, '5559edd')
        System.setProperty(Constants.ENV_VAR_FLOW_ID, flowId)
        System.setProperty(Constants.ENV_VAR_STEP_ID, 'step_1')
        System.setProperty(Constants.ENV_VAR_USER_ID, '5559edd38968ec0736000002')
        System.setProperty(Constants.ENV_VAR_COMP_ID, '5559edd38968ec0736000456')
        System.setProperty(Constants.ENV_VAR_EXEC_ID, 'some-exec-id')
        System.setProperty(Constants.ENV_VAR_CONTAINER_ID, 'container_12345')
        System.setProperty(Constants.ENV_VAR_WORKSPACE_ID, "workspace_123")



        stepCfg = Json.createObjectBuilder()
                .add('apiKey', 'secret')
                .add(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, 'testCryptoPassword')
                .add(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, 'iv=any16_symbols')
                .add(Constants.ENV_VAR_AMQP_URI, 'amqp://guest:guest@localhost:5672')
                .add(Constants.ENV_VAR_PUBLISH_MESSAGES_TO, prefix + ':exchange')
                .add(Constants.ENV_VAR_DATA_ROUTING_KEY, prefix + ':routing_key:message')
                .add(Constants.ENV_VAR_LISTEN_MESSAGES_ON, prefix + ':messages')
                .build()

        cipher = new CryptoServiceImpl(
                stepCfg.getString(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD),
                stepCfg.getString(Constants.ENV_VAR_MESSAGE_CRYPTO_IV))

        amqp = new AmqpServiceImpl(cipher)

        amqp.setAmqpUri(stepCfg.getString(Constants.ENV_VAR_AMQP_URI))
        amqp.setSubscribeExchangeName(stepCfg.getString(Constants.ENV_VAR_LISTEN_MESSAGES_ON))
        amqp.setPrefetchCount(1)

        amqp.connectAndSubscribe()

        amqp.subscribeChannel.exchangeDeclare(
                stepCfg.getString(Constants.ENV_VAR_LISTEN_MESSAGES_ON), 'direct', true, false, [:])
        amqp.subscribeChannel.queueDeclare(
                stepCfg.getString(Constants.ENV_VAR_LISTEN_MESSAGES_ON), true, false, false, [:])
        amqp.subscribeChannel.queueBind(
                stepCfg.getString(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                stepCfg.getString(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                stepCfg.getString(Constants.ENV_VAR_DATA_ROUTING_KEY)
        )

        def messagePublisher = new MessagePublisherImpl(
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                Integer.MAX_VALUE,
                100, 5 * 60 * 1000, true, amqp)

        publishChannel = messagePublisher.getPublishChannel()

        publishChannel.exchangeDeclare(
                stepCfg.getString(Constants.ENV_VAR_PUBLISH_MESSAGES_TO), 'direct', true, false, [:])
        publishChannel.queueDeclare(dataQueue, true, false, false, [:])

        publishChannel.queueBind(
                dataQueue,
                stepCfg.getString(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                stepCfg.getString(Constants.ENV_VAR_DATA_ROUTING_KEY)
        )

        publishChannel.queuePurge(stepCfg.getString(Constants.ENV_VAR_LISTEN_MESSAGES_ON))

        server = new Server(8182);

        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {

                def jsonResponse = createResponse(target, baseRequest)

                response.setHeader("Content-type", 'application/json')
                response.getOutputStream().write(JSON.stringify(jsonResponse).getBytes());
                response.getOutputStream().close();
            }

            private JsonObject createResponse(final String target, final Request baseRequest) {
                def flowId = ShutdownIntegrationSpec.this.flowId
                if ("/v1/tasks/${flowId}/steps/step_1".toString().equals(target)) {

                    return Json.createObjectBuilder()
                            .add(Constants.STEP_PROPERTY_ID, System.getProperty(Constants.ENV_VAR_STEP_ID))
                            .add(Constants.STEP_PROPERTY_COMP_ID, System.getProperty(Constants.ENV_VAR_COMP_ID))
                            .add(Constants.STEP_PROPERTY_FUNCTION, System.getProperty(Constants.ENV_VAR_FUNCTION))
                            .add(Constants.STEP_PROPERTY_CFG, ShutdownIntegrationSpec.this.stepCfg)
                            .add(Constants.STEP_PROPERTY_SNAPSHOT, Json.createObjectBuilder().add('lastModifiedDate', 123456789).build())
                            .add(Constants.STEP_PROPERTY_PASSTHROUGH, true)
                            .build()
                }
                if ("/sailor-support/hooks/task/${flowId}/startup/data".toString().equals(target)) {
                    if (baseRequest.getMethod().equalsIgnoreCase("post")) {
                        def payload = Json.createReader(baseRequest.getInputStream()).readObject()
                        ShutdownIntegrationSpec.this.startupPayload = payload
                        return Json.createObjectBuilder()
                                .add("taskId", flowId)
                                .add("payload", payload)
                                .build()
                    }
                    else {
                        if (baseRequest.getMethod().equalsIgnoreCase("delete")) {
                            ShutdownIntegrationSpec.this.shutdownFlowId = flowId
                        }
                        return Json.createObjectBuilder()
                                .add("method", baseRequest.getMethod())
                                .build()
                    }
                }

                throw new IllegalStateException("Target not supported yet:" + target);
            }
        });

        server.start()
    }

    def cleanupSpec() {
        server.stop()
    }

    def cleanup() {
        System.clearProperty(Constants.ENV_VAR_HOOK_SHUTDOWN)
        startupPayload = null
        shutdownFlowId = null
    }

    def "should execute shutdown successfully"() {
        def blockingVar = new BlockingVariable(20)
        setup:
        System.setProperty(Constants.ENV_VAR_HOOK_SHUTDOWN, "1");
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'startupShutdownAction')

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                ShutdownIntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def bodyString = new String(body, "UTF-8");
                def message = JSON.parseObject(bodyString)
                blockingVar.set([message:message, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)

        then: "Blocking var exists"
        def result = blockingVar.get()

        then: "Emitted message is received"
        result.message.getString("shutdownSignal") == "1"
        result.message.configuration == stepCfg

        then: "Shutdown payload sent to API"
        shutdownFlowId == System.getProperty(Constants.ENV_VAR_FLOW_ID)

        cleanup:
        sailor.amqp == null
        publishChannel.basicCancel(consumerTag)
    }
}