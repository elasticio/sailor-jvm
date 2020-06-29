package io.elastic.sailor

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.elastic.api.HttpReply
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.component.StartupShutdownAction
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
class FlowControlIntegrationSpec extends Specification {

    @Shared
    def amqp

    @Shared
    def cipher

    @Shared
    def prefix = 'sailor_jvm_flow_control_integration_test'

    @Shared
    def dataQueue = prefix + '_queue'

    @Shared
    def errorsQueue = prefix + '_queue_errors'

    @Shared
    def traceId = prefix + '_trace_id_123456'

    @Shared
    def flowId = '5559edd38968ec0736000003'

    @Shared
    def stepCfg

    @Shared
    def messageId = UUID.randomUUID().toString()

    @Shared
    def sailor;

    @Shared
    Server server

    @Shared
    Channel publishChannel

    def setupSpec() {

        System.setProperty(Constants.ENV_VAR_API_URI, 'http://localhost:8182')
        System.setProperty(Constants.ENV_VAR_API_USERNAME, 'test@test.com')
        System.setProperty(Constants.ENV_VAR_API_KEY, '5559edd')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, 'testCryptoPassword')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, 'iv=any16_symbols')
        System.setProperty(Constants.ENV_VAR_FLOW_ID, flowId)
        System.setProperty(Constants.ENV_VAR_STEP_ID, 'step_1')
        System.setProperty(Constants.ENV_VAR_USER_ID, '5559edd38968ec0736000002')
        System.setProperty(Constants.ENV_VAR_COMP_ID, '5559edd38968ec0736000456')
        System.setProperty(Constants.ENV_VAR_EXEC_ID, 'some-exec-id')
        System.setProperty(Constants.ENV_VAR_CONTAINER_ID, 'container_12345')
        System.setProperty(Constants.ENV_VAR_WORKSPACE_ID, "workspace_123")

        System.setProperty(Constants.ENV_VAR_AMQP_URI, 'amqp://guest:guest@localhost:5672')
        System.setProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON, prefix + ':messages')
        System.setProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO, prefix + ':exchange')
        System.setProperty(Constants.ENV_VAR_DATA_ROUTING_KEY, prefix + ':routing_key:message')
        System.setProperty(Constants.ENV_VAR_ERROR_ROUTING_KEY, prefix + ':routing_key:error')
        System.setProperty(Constants.ENV_VAR_REBOUND_ROUTING_KEY, prefix + ':routing_key:rebound')
        System.setProperty(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY, prefix + ':routing_key:snapshot')

        System.setProperty(Constants.ENV_VAR_AMQP_PUBLISH_RETRY_ATTEMPTS, '2')


        stepCfg = Json.createObjectBuilder()
                .add('apiKey', 'secret')
                .add(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD))
                .add(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV))
                .add(Constants.ENV_VAR_AMQP_URI, System.getProperty(Constants.ENV_VAR_AMQP_URI))
                .add(Constants.ENV_VAR_PUBLISH_MESSAGES_TO, System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO))
                .add(Constants.ENV_VAR_DATA_ROUTING_KEY, System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY))
                .build()

        cipher = new CryptoServiceImpl(
                System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD),
                System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV))

        amqp = new AmqpServiceImpl(cipher)

        amqp.setAmqpUri(System.getProperty(Constants.ENV_VAR_AMQP_URI))
        amqp.setSubscribeExchangeName(System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON))
        amqp.setPrefetchCount(1)

        amqp.connectAndSubscribe()

        amqp.subscribeChannel.exchangeDeclare(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON), 'direct', true, false, [:])
        amqp.subscribeChannel.queueDeclare(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON), true, false, false, [:])
        amqp.subscribeChannel.queueBind(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY)
        )

        def messagePublisher = new MessagePublisherImpl(
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                Integer.MAX_VALUE,
                100, 5 * 60 * 1000, amqp)

        publishChannel = messagePublisher.getPublishChannel()

        def queueArgs = ['x-max-length': 2, 'x-overflow': 'reject-publish']

        publishChannel.exchangeDeclare(
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO), 'direct', true, false, [:])
        publishChannel.queueDeclare(dataQueue, true, false, false, queueArgs)
        publishChannel.queueDeclare(errorsQueue, true, false, false, [:])

        publishChannel.queueBind(
                dataQueue,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY)
        )

        publishChannel.queueBind(
                errorsQueue,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                System.getProperty(Constants.ENV_VAR_ERROR_ROUTING_KEY)
        )

        publishChannel.queuePurge(System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON))

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
                def flowId = FlowControlIntegrationSpec.this.flowId
                if ("/v1/tasks/${flowId}/steps/step_1".toString().equals(target)) {

                    return Json.createObjectBuilder()
                            .add(Constants.STEP_PROPERTY_ID, System.getProperty(Constants.ENV_VAR_STEP_ID))
                            .add(Constants.STEP_PROPERTY_COMP_ID, System.getProperty(Constants.ENV_VAR_COMP_ID))
                            .add(Constants.STEP_PROPERTY_FUNCTION, System.getProperty(Constants.ENV_VAR_FUNCTION))
                            .add(Constants.STEP_PROPERTY_CFG, FlowControlIntegrationSpec.this.stepCfg)
                            .add(Constants.STEP_PROPERTY_SNAPSHOT, Json.createObjectBuilder().add('lastModifiedDate', 123456789).build())
                            .add(Constants.STEP_PROPERTY_PASSTHROUGH, true)
                            .build()
                }
                if ("/sailor-support/hooks/task/${flowId}/startup/data".toString().equals(target)) {
                    if (baseRequest.getMethod().equalsIgnoreCase("post")) {
                        def payload = Json.createReader(baseRequest.getInputStream()).readObject()
                        return Json.createObjectBuilder()
                                .add("taskId", flowId)
                                .add("payload", payload)
                                .build()
                    }
                    else {
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
        System.clearProperty(Constants.ENV_VAR_STARTUP_REQUIRED)
        System.clearProperty(Constants.ENV_VAR_HOOK_SHUTDOWN)
    }

    def "should retry messages"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'helloworldaction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
                "source": "Integration Test (Flow Control)",
                (Constants.AMQP_META_HEADER_TRACE_ID): traceId
        ]

        def options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .priority(1)
                .deliveryMode(2)
                .build()

        def msg = new Message.Builder()
                .body(Json.createObjectBuilder().add('message', "Let's see flow control in action!").build())
                .build()

        byte[] payload = cipher.encryptMessage(msg).getBytes();

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload)

        amqp.subscribeChannel.queuePurge(dataQueue)
        amqp.subscribeChannel.queuePurge(errorsQueue)

        1.upto(3) {
            publishChannel.basicPublish(
                    System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                    System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                    options,
                    payload)
        }

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                FlowControlIntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def errorJson = JSON.parseObject(new String(body, "UTF-8"))
                def error = FlowControlIntegrationSpec.this.cipher.decrypt(errorJson.getString('error'));
                blockingVar.set([error:error, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(errorsQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)


        then: "AMQP properties headers are all set"
        def result = blockingVar.get()

        println result.properties
        result.properties.headers.size() == 13
        result.properties.headers.start != null
        result.properties.headers.compId.toString() == '5559edd38968ec0736000456'
        result.properties.headers.function.toString() == System.getProperty(Constants.ENV_VAR_FUNCTION)
        result.properties.headers.stepId.toString() == "step_1"
        result.properties.headers.userId.toString() == "5559edd38968ec0736000002"
        result.properties.headers.taskId.toString() == headers.taskId
        result.properties.headers.execId.toString() == headers.execId
        result.properties.headers.containerId.toString() == 'container_12345'
        result.properties.headers.workspaceId.toString() == 'workspace_123'
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.threadId.toString() == traceId


        then: "Emitted error received"
        def errorJson = JSON.parseObject(result.error);
        errorJson.getString('name') == 'java.lang.IllegalStateException'
        errorJson.getString('message') == 'Failed to publish the message to a queue after 2 retries. The limit of 2 retries reached.'
        errorJson.getString('stack').startsWith('java.lang.IllegalStateException: Failed to publish the message to a queue after 2 retries. The limit of 2 retries reached.')

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }
}