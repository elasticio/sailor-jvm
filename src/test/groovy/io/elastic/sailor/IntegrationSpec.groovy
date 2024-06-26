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
import io.elastic.sailor.impl.MessageEncoding
import io.elastic.sailor.impl.MessageFormat
import io.elastic.sailor.impl.MessagePublisherImpl
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.concurrent.BlockingVariable

import jakarta.json.Json
import jakarta.json.JsonObject
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@Stepwise
class IntegrationSpec extends Specification {

    @Shared
    def amqp

    @Shared
    def cipher

    @Shared
    def prefix = 'sailor_jvm_integration_test'

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
    def threadId = prefix + '_thread_id_123456'

    @Shared
    def messageId = UUID.randomUUID().toString()

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
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, 'testCryptoPassword')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, 'iv=any16_symbols')
        System.setProperty(Constants.ENV_VAR_FLOW_ID, flowId)
        System.setProperty(Constants.ENV_VAR_STEP_ID, 'step_1')
        System.setProperty(Constants.ENV_VAR_USER_ID, '5559edd38968ec0736000002')
        System.setProperty(Constants.ENV_VAR_COMP_ID, '5559edd38968ec0736000456')
        System.setProperty(Constants.ENV_VAR_EXEC_ID, 'some-exec-id')
        System.setProperty(Constants.ENV_VAR_CONTAINER_ID, 'container_12345')
        System.setProperty(Constants.ENV_VAR_WORKSPACE_ID, "workspace_123")
        System.setProperty(Constants.ENV_VAR_INPUT_FORMAT, MessageFormat.DEFAULT.name())

        System.setProperty(Constants.ENV_VAR_AMQP_URI, 'amqp://guest:guest@localhost:5672')
        System.setProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON, prefix + ':messages')
        System.setProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO, prefix + ':exchange')
        System.setProperty(Constants.ENV_VAR_DATA_ROUTING_KEY, prefix + ':routing_key:message')
        System.setProperty(Constants.ENV_VAR_ERROR_ROUTING_KEY, prefix + ':routing_key:error')
        System.setProperty(Constants.ENV_VAR_REBOUND_ROUTING_KEY, prefix + ':routing_key:rebound')
        System.setProperty(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY, prefix + ':routing_key:snapshot')


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
        amqp.setThreadPoolSize(1)

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
                100, 5 * 60 * 1000, true, true, amqp)

        publishChannel = messagePublisher.getPublishChannel()

        publishChannel.exchangeDeclare(
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO), 'direct', true, false, [:])
        publishChannel.queueDeclare(dataQueue, true, false, false, [:])
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
                def flowId = IntegrationSpec.this.flowId
                if ("/v1/tasks/${flowId}/steps/step_1".toString().equals(target)) {

                    return Json.createObjectBuilder()
                            .add(Constants.STEP_PROPERTY_ID, System.getProperty(Constants.ENV_VAR_STEP_ID))
                            .add(Constants.STEP_PROPERTY_COMP_ID, System.getProperty(Constants.ENV_VAR_COMP_ID))
                            .add(Constants.STEP_PROPERTY_FUNCTION, System.getProperty(Constants.ENV_VAR_FUNCTION))
                            .add(Constants.STEP_PROPERTY_CFG, IntegrationSpec.this.stepCfg)
                            .add(Constants.STEP_PROPERTY_SNAPSHOT, Json.createObjectBuilder().add('lastModifiedDate', 123456789).build())
                            .add(Constants.STEP_PROPERTY_PASSTHROUGH, true)
                            .build()
                }
                if ("/sailor-support/hooks/task/${flowId}/startup/data".toString().equals(target)) {
                    if (baseRequest.getMethod().equalsIgnoreCase("post")) {
                        def payload = Json.createReader(baseRequest.getInputStream()).readObject()
                        IntegrationSpec.this.startupPayload = payload
                        return Json.createObjectBuilder()
                                .add("taskId", flowId)
                                .add("payload", payload)
                                .build()
                    }
                    else {
                        if (baseRequest.getMethod().equalsIgnoreCase("delete")) {
                            IntegrationSpec.this.shutdownFlowId = flowId
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
        System.clearProperty(Constants.ENV_VAR_STARTUP_REQUIRED)
        System.clearProperty(Constants.ENV_VAR_HOOK_SHUTDOWN)
        startupPayload = null
        shutdownFlowId = null
    }

    def "run sailor successfully"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'helloworldaction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
                "source": "Integration Test",
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
                .body(Json.createObjectBuilder().add('message', 'Just do it!').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), false)
                def testMsg = Utils.createMessage(
                        IntegrationSpec.this.cipher.decryptMessageContent(body, MessageEncoding.BASE64))
                blockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)


        then: "AMQP properties headers are all set"
        def result = blockingVar.get()

        result.properties.headers.size() == 14
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
        result.properties.headers.messageId.toString() == result.message.id.toString()
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.protocolVersion == 1

        then: "Emitted message is received"
        result.message.headers.isEmpty()
        JSON.stringify(result.message.body) == '{"echo":{"message":"Just do it!"}}'

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }

    def "run sailor successfully with threads"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'helloworldaction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
                (Constants.AMQP_META_HEADER_TRACE_ID): traceId,
                (Constants.AMQP_HEADER_THREAD_ID): threadId
        ]

        def options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .priority(1)
                .deliveryMode(2)
                .build()

        def msg = new Message.Builder()
                .body(Json.createObjectBuilder().add('message', 'Just do it!').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def testMsg = Utils.createMessage(
                        IntegrationSpec.this.cipher.decryptMessageContent(body, MessageEncoding.BASE64))
                blockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)


        then: "AMQP properties headers are all set"
        def result = blockingVar.get()

        result.properties.headers.size() == 14
        result.properties.headers.start != null
        result.properties.headers.compId.toString() == '5559edd38968ec0736000456'
        result.properties.headers.function.toString() == headers.function
        result.properties.headers.stepId.toString() == "step_1"
        result.properties.headers.userId.toString() == "5559edd38968ec0736000002"
        result.properties.headers.taskId.toString() == headers.taskId
        result.properties.headers.execId.toString() == headers.execId
        result.properties.headers.containerId.toString() == 'container_12345'
        result.properties.headers.workspaceId.toString() == 'workspace_123'
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.threadId.toString() == threadId
        result.properties.headers.messageId.toString() == result.message.id.toString()
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.protocolVersion == 1

        then: "Emitted message is received"
        result.message.headers.isEmpty()
        JSON.stringify(result.message.body) == '{"echo":{"message":"Just do it!"}}'

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }

    def "run sailor successfully and pass through"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'helloworldaction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
                (Constants.AMQP_META_HEADER_TRACE_ID): traceId
        ]

        def options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .priority(1)
                .deliveryMode(2)
                .build()

        def passedThroughMessage = new Message.Builder()
                .body(Json.createObjectBuilder().add('greetings', 'Hello from trigger').build())
                .build()
                .toJsonObject()
        def msgBody = Json.createObjectBuilder().add('message', 'Just do it!').build();

        def passthrough = Json.createObjectBuilder()
                .add('step_0', passedThroughMessage)
                .build();

        def msg = Json.createObjectBuilder()
                .add(Message.PROPERTY_BODY, msgBody)
                .add(Message.PROPERTY_PASSTHROUGH, passthrough)
                .build()

        byte[] payload = cipher.encryptJsonObject(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def decryptedJson = IntegrationSpec.this.cipher.decryptMessageContent(body, MessageEncoding.BASE64)
                def testMsg = Utils.createMessage(decryptedJson)
                def receivedPassthrough = decryptedJson.getJsonObject(Message.PROPERTY_PASSTHROUGH)
                blockingVar.set([message:testMsg, properties:properties, receivedPassthrough:receivedPassthrough]);
            }
        }

        def consumerTag = publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)


        then: "AMQP properties headers are all set"
        def result = blockingVar.get()

        result.properties.headers.size() == 14
        result.properties.headers.start != null
        result.properties.headers.compId.toString() == '5559edd38968ec0736000456'
        result.properties.headers.function.toString() == headers.function
        result.properties.headers.stepId.toString() == "step_1"
        result.properties.headers.userId.toString() == "5559edd38968ec0736000002"
        result.properties.headers.taskId.toString() == headers.taskId
        result.properties.headers.execId.toString() == headers.execId
        result.properties.headers.containerId.toString() == 'container_12345'
        result.properties.headers.workspaceId.toString() == 'workspace_123'
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() == result.message.id.toString()
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.threadId.toString() == traceId
        result.properties.headers.protocolVersion == 1

        then: "Emitted message is received"
        result.message.headers.isEmpty()
        JSON.stringify(result.message.body) == '{"echo":{"message":"Just do it!"}}'
        JSON.stringify(result.receivedPassthrough.get('step_0').get('body')) == '{"greetings":"Hello from trigger"}'
        JSON.stringify(result.receivedPassthrough.get('step_1').get('body')) == '{"echo":{"message":"Just do it!"}}'

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }

    def "should execute startup/init successfully"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_STARTUP_REQUIRED, "1");
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'startupInitAction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
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
                .body(Json.createObjectBuilder().add('message', 'Show me startup/init').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def testMsg = Utils.createMessage(
                        IntegrationSpec.this.cipher.decryptMessageContent(body, MessageEncoding.BASE64))
                blockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)

        then: "AMQP properties headers are all set"
        def result = blockingVar.get()

        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() == result.message.id.toString()
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.containerId.toString() == 'container_12345'

        then: "Emitted message is received"
        result.message.headers.isEmpty()
        result.message.body.echo == msg.body
        result.message.body.startupAndInit.startup == stepCfg
        result.message.body.startupAndInit.init == stepCfg

        then: "Startup payload is not sent to API"
        startupPayload != null
        startupPayload.isEmpty()

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }

    def "should execute startup successfully"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_STARTUP_REQUIRED, "1");
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'startupShutdownAction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
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
                .body(Json.createObjectBuilder().add('message', 'Show me startup/shutdown').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def testMsg = Utils.createMessage(
                        IntegrationSpec.this.cipher.decryptMessageContent(body, MessageEncoding.BASE64))
                blockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)

        then: "AMQP properties headers are all set"
        def result = blockingVar.get()

        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() == result.message.id.toString()
        result.properties.headers.parentMessageId.toString() == messageId

        then: "Emitted message is received"
        result.message.headers.isEmpty()
        result.message.body.echo == msg.body
        result.message.body.startupAndShutdown.startup == stepCfg

        then: "Startup payload sent to API"
        def expectedPayload = Json.createObjectBuilder()
                .add("subscriptionId", StartupShutdownAction.SUBSCRIPTION_ID)
                .build()
        JSON.stringify(startupPayload) == JSON.stringify(expectedPayload)

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }

    def "should execute shutdown successfully"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_HOOK_SHUTDOWN, "1");
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'startupShutdownAction')

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def bodyString = new String(body, "UTF-8");
                def testMsg = JSON.parseObject(bodyString)
                blockingVar.set([message:testMsg, properties:properties]);
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

    def "should send http reply successfully"() {
        def blockingVar = new BlockingVariable(5)

        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'httpReplyAction')

        def replyQueueName = prefix + '_request_reply_queue'
        def replyQueueRoutingKey = prefix + '_request_reply_routing_key'

        publishChannel.queueDeclare(replyQueueName, true, false, false, [:])
        publishChannel.queueBind(
                replyQueueName,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                replyQueueRoutingKey)


        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                'reply_to': replyQueueRoutingKey,
                messageId: messageId,
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
                .body(Json.createObjectBuilder().add('message', 'Send me a reply').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def testMsg = IntegrationSpec.this.cipher.decrypt(body, MessageEncoding.BASE64);
                blockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(replyQueueName, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)

        then: "AMQP properties headers are all set"
        def result = blockingVar.get()
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() != null
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.containerId.toString() == 'container_12345'

        then: "Emitted message is received"
        def message = JSON.parseObject(result.message)
        message.statusCode.intValue() == HttpReply.Status.ACCEPTED.statusCode
        message.getJsonString('body').getString() == '{"echo":{"message":"Send me a reply"}}'
        JSON.stringify(message.get('headers')) == '{"Content-type":"application/json","x-custom-header":"abcdef"}'

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }

    def "should send error http reply successfully"() {
        def httpReplyBlockingVar = new BlockingVariable(5)
        def errorBlockingVar = new BlockingVariable(5)

        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'erroneousAction')

        def replyQueueName = prefix + '_request_reply_queue_error'
        def replyQueueRoutingKey = prefix + '_request_reply_error_routing_key'

        publishChannel.queueDeclare(replyQueueName, true, false, false, [:])
        publishChannel.queueBind(
                replyQueueName,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                replyQueueRoutingKey)


        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                (Constants.AMQP_HEADER_REPLY_TO): replyQueueRoutingKey,
                messageId: messageId,
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
                .body(Json.createObjectBuilder().add('message', 'Send me a reply').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def httpReplyConsumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def testMsg = IntegrationSpec.this.cipher.decrypt(body, MessageEncoding.BASE64);
                httpReplyBlockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def httpReplyConsumerTag = publishChannel.basicConsume(replyQueueName, httpReplyConsumer)

        def errorConsumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def errJson = JSON.parseObject(new String(body, "UTF-8"))
                def error = IntegrationSpec.this.cipher.decrypt(
                        errJson.getString('error').getBytes(), MessageEncoding.BASE64);
                errorBlockingVar.set([error:error, properties:properties]);
            }
        }

        def errorConsumerTag = publishChannel.basicConsume(errorsQueue, errorConsumer)

        when:

        sailor = Sailor.createAndStartSailor(false)

        then: "AMQP properties headers are all set"
        def result = httpReplyBlockingVar.get()
        result.properties.headers[Constants.AMQP_HEADER_ERROR_RESPONSE] == true
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() != null
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.containerId.toString() == 'container_12345'

        then: "Emitted message is received"
        def message = JSON.parseObject(result.message)
        message.getString('name') == 'java.lang.RuntimeException'
        message.getString('stack').startsWith('java.lang.RuntimeException: Ouch. Something went wrong')
        message.getString('message') == 'Ouch. Something went wrong'

        then:
        def errorResult = errorBlockingVar.get()
        errorResult.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        errorResult.properties.headers.messageId.toString() != null
        errorResult.properties.headers.parentMessageId.toString() == messageId
        errorResult.properties.headers.containerId.toString() == System.getProperty(Constants.ENV_VAR_CONTAINER_ID)
        errorResult.properties.headers.workspaceId.toString() == System.getProperty(Constants.ENV_VAR_WORKSPACE_ID)
        errorResult.properties.headers.execId.toString() == System.getProperty(Constants.ENV_VAR_EXEC_ID)
        errorResult.properties.headers.taskId.toString() == System.getProperty(Constants.ENV_VAR_FLOW_ID)
        errorResult.properties.headers.userId.toString() == System.getProperty(Constants.ENV_VAR_USER_ID)
        errorResult.properties.headers.stepId.toString() == System.getProperty(Constants.ENV_VAR_STEP_ID)
        errorResult.properties.headers.compId.toString() == System.getProperty(Constants.ENV_VAR_COMP_ID)
        errorResult.properties.headers.function.toString() == System.getProperty(Constants.ENV_VAR_FUNCTION)

        then: "Emitted error received"
        def errorJson = JSON.parseObject(errorResult.error);
        errorJson.getString('name') == 'java.lang.RuntimeException'
        errorJson.getString('message') == 'Ouch. Something went wrong'
        errorJson.getString('stack').startsWith('java.lang.RuntimeException: Ouch. Something went wrong')

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(httpReplyConsumerTag)
        publishChannel.basicCancel(errorConsumerTag)
    }

    def "should send error http reply successfully - protocol version 2"() {
        def httpReplyBlockingVar = new BlockingVariable(5)
        def errorBlockingVar = new BlockingVariable(5)

        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'erroneousAction')

        def replyQueueName = prefix + '_request_reply_queue_error'
        def replyQueueRoutingKey = prefix + '_request_reply_error_routing_key'

        publishChannel.queueDeclare(replyQueueName, true, false, false, [:])
        publishChannel.queueBind(
                replyQueueName,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                replyQueueRoutingKey)


        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                (Constants.AMQP_HEADER_REPLY_TO): replyQueueRoutingKey,
                messageId: messageId,
                (Constants.AMQP_META_HEADER_TRACE_ID): traceId,
                (Constants.AMQP_HEADER_PROTOCOL_VERSION): MessageEncoding.UTF8.protocolVersion
        ]

        def options = new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .headers(headers)
                .priority(1)
                .deliveryMode(2)
                .build()

        def msg = new Message.Builder()
                .body(Json.createObjectBuilder().add('message', 'Send me a reply').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.UTF8)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def httpReplyConsumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def testMsg = IntegrationSpec.this.cipher.decrypt(body, MessageEncoding.BASE64);
                httpReplyBlockingVar.set([message:testMsg, properties:properties]);
            }
        }

        def httpReplyConsumerTag = publishChannel.basicConsume(replyQueueName, httpReplyConsumer)

        def errorConsumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def errJson = JSON.parseObject(new String(body, "UTF-8"))
                def error = IntegrationSpec.this.cipher.decrypt(
                        errJson.getString('error').getBytes(), MessageEncoding.BASE64);
                errorBlockingVar.set([error:error, properties:properties]);
            }
        }

        def errorConsumerTag = publishChannel.basicConsume(errorsQueue, errorConsumer)

        when:

        sailor = Sailor.createAndStartSailor(false)


        then: "AMQP properties headers are all set"
        def result = httpReplyBlockingVar.get()
        result.properties.headers[Constants.AMQP_HEADER_ERROR_RESPONSE] == true
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() != null
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.containerId.toString() == 'container_12345'

        then: "Emitted message is received"
        def message = JSON.parseObject(result.message)
        message.getString('name') == 'java.lang.RuntimeException'
        message.getString('stack').startsWith('java.lang.RuntimeException: Ouch. Something went wrong')
        message.getString('message') == 'Ouch. Something went wrong'

        then:
        def errorResult = errorBlockingVar.get()
        errorResult.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        errorResult.properties.headers.messageId.toString() != null
        errorResult.properties.headers.parentMessageId.toString() == messageId
        errorResult.properties.headers.containerId.toString() == System.getProperty(Constants.ENV_VAR_CONTAINER_ID)
        errorResult.properties.headers.workspaceId.toString() == System.getProperty(Constants.ENV_VAR_WORKSPACE_ID)
        errorResult.properties.headers.execId.toString() == System.getProperty(Constants.ENV_VAR_EXEC_ID)
        errorResult.properties.headers.taskId.toString() == System.getProperty(Constants.ENV_VAR_FLOW_ID)
        errorResult.properties.headers.userId.toString() == System.getProperty(Constants.ENV_VAR_USER_ID)
        errorResult.properties.headers.stepId.toString() == System.getProperty(Constants.ENV_VAR_STEP_ID)
        errorResult.properties.headers.compId.toString() == System.getProperty(Constants.ENV_VAR_COMP_ID)
        errorResult.properties.headers.function.toString() == System.getProperty(Constants.ENV_VAR_FUNCTION)

        then: "Emitted error received"
        def errorJson = JSON.parseObject(errorResult.error);
        errorJson.getString('name') == 'java.lang.RuntimeException'
        errorJson.getString('message') == 'Ouch. Something went wrong'
        errorJson.getString('stack').startsWith('java.lang.RuntimeException: Ouch. Something went wrong')

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(httpReplyConsumerTag)
        publishChannel.basicCancel(errorConsumerTag)
    }

    def "publish init errors to RabbitMQ"() {
        def blockingVar = new BlockingVariable(5)
        setup:
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'erroneousAction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty(Constants.ENV_VAR_USER_ID),
                start     : System.currentTimeMillis(),
                messageId: messageId,
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
                .body(Json.createObjectBuilder().add('message', 'Just do it 2!').build())
                .build()

        byte[] payload = cipher.encryptMessage(msg, MessageEncoding.BASE64)

        publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(publishChannel) {
            @Override
            public void handleDelivery(String tag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def errJson = JSON.parseObject(new String(body, "UTF-8"))
                def error = IntegrationSpec.this.cipher.decrypt(
                        errJson.getString('error').getBytes(), MessageEncoding.BASE64);
                blockingVar.set([error:error, properties:properties]);
            }
        }

        def consumerTag = publishChannel.basicConsume(errorsQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor(false)


        then: "AMQP properties headers are all set"
        def result = blockingVar.get()
        result.properties.headers[Constants.AMQP_META_HEADER_TRACE_ID].toString() == traceId
        result.properties.headers.messageId.toString() != null
        result.properties.headers.parentMessageId.toString() == messageId
        result.properties.headers.containerId.toString() == System.getProperty(Constants.ENV_VAR_CONTAINER_ID)
        result.properties.headers.workspaceId.toString() == System.getProperty(Constants.ENV_VAR_WORKSPACE_ID)
        result.properties.headers.execId.toString() == System.getProperty(Constants.ENV_VAR_EXEC_ID)
        result.properties.headers.taskId.toString() == System.getProperty(Constants.ENV_VAR_FLOW_ID)
        result.properties.headers.userId.toString() == System.getProperty(Constants.ENV_VAR_USER_ID)
        result.properties.headers.stepId.toString() == System.getProperty(Constants.ENV_VAR_STEP_ID)
        result.properties.headers.compId.toString() == System.getProperty(Constants.ENV_VAR_COMP_ID)
        result.properties.headers.function.toString() == System.getProperty(Constants.ENV_VAR_FUNCTION)

        then: "Emitted error received"
        def errorJson = JSON.parseObject(result.error);
        errorJson.getString('name') == 'java.lang.RuntimeException'
        errorJson.getString('message') == 'Ouch. Something went wrong'
        errorJson.getString('stack').startsWith('java.lang.RuntimeException: Ouch. Something went wrong')

        cleanup:
        sailor.amqp.cancelConsumer()
        publishChannel.basicCancel(consumerTag)
    }
}
