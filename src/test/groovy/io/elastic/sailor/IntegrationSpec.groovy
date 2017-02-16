package io.elastic.sailor

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.elastic.api.HttpReply
import io.elastic.api.JSON
import io.elastic.api.Message
import io.elastic.sailor.impl.AmqpServiceImpl
import io.elastic.sailor.impl.CryptoServiceImpl
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Stepwise
import spock.util.concurrent.BlockingVariable

import javax.json.Json
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
    def blockingVar

    @Shared
    def sailor;

    def setupSpec() {
        System.setProperty(Constants.ENV_VAR_API_URI, 'http://localhost:8182')
        System.setProperty(Constants.ENV_VAR_API_USERNAME, 'test@test.com')
        System.setProperty(Constants.ENV_VAR_API_KEY, '5559edd')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, 'testCryptoPassword')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, 'iv=any16_symbols')
        System.setProperty(Constants.ENV_VAR_FLOW_ID, '5559edd38968ec0736000003')
        System.setProperty(Constants.ENV_VAR_STEP_ID, 'step_1')
        System.setProperty(Constants.ENV_VAR_USER_ID, '5559edd38968ec0736000002')
        System.setProperty(Constants.ENV_VAR_COMP_ID, '5559edd38968ec0736000456')
        System.setProperty(Constants.ENV_VAR_EXEC_ID, 'some-exec-id')

        System.setProperty(Constants.ENV_VAR_AMQP_URI, 'amqp://guest:guest@localhost:5672')
        System.setProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON, prefix + ':messages')
        System.setProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO, prefix + ':exchange')
        System.setProperty(Constants.ENV_VAR_DATA_ROUTING_KEY, prefix + ':routing_key:message')
        System.setProperty(Constants.ENV_VAR_ERROR_ROUTING_KEY, prefix + ':routing_key:error')
        System.setProperty(Constants.ENV_VAR_REBOUND_ROUTING_KEY, prefix + ':routing_key:rebound')
        System.setProperty(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY, prefix + ':routing_key:snapshot')

        cipher = new CryptoServiceImpl(
                System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD),
                System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV))

        amqp = new AmqpServiceImpl(cipher)

        amqp.setAmqpUri(System.getProperty(Constants.ENV_VAR_AMQP_URI))
        amqp.setPublishExchangeName(System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO))
        amqp.setSubscribeExchangeName(System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON))
        amqp.setPrefetchCount(1)

        amqp.connect()

        amqp.subscribeChannel.exchangeDeclare(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON), 'direct', true, false, [:])
        amqp.subscribeChannel.queueDeclare(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON), true, false, false, [:])
        amqp.subscribeChannel.queueBind(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY)
        )

        amqp.publishChannel.exchangeDeclare(
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO), 'direct', true, false, [:])
        amqp.publishChannel.queueDeclare(dataQueue, true, false, false, [:])
        amqp.publishChannel.queueDeclare(errorsQueue, true, false, false, [:])

        amqp.publishChannel.queueBind(
                dataQueue,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY)
        )

        amqp.publishChannel.queueBind(
                errorsQueue,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                System.getProperty(Constants.ENV_VAR_ERROR_ROUTING_KEY)
        )

        Server server = new Server(8182);
        server.setHandler(new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest,
                               HttpServletRequest request,
                               HttpServletResponse response)
                    throws IOException, ServletException {

                def stepData = Json.createObjectBuilder()
                        .add(Constants.STEP_PROPERTY_ID, System.getProperty(Constants.ENV_VAR_STEP_ID))
                        .add(Constants.STEP_PROPERTY_COMP_ID, System.getProperty(Constants.ENV_VAR_COMP_ID))
                        .add(Constants.STEP_PROPERTY_FUNCTION, System.getProperty(Constants.ENV_VAR_FUNCTION))
                        .add(Constants.STEP_PROPERTY_CFG, Json.createObjectBuilder().add('apiKey', 'secret').build())
                        .add(Constants.STEP_PROPERTY_SNAPSHOT,
                        Json.createObjectBuilder().add('lastModifiedDate', 123456789).build())
                        .build();
                response.setHeader("Content-type", 'application/json')
                response.getOutputStream().write(JSON.stringify(stepData).getBytes());
                response.getOutputStream().close();
            }
        });
        server.start();
    }

    def cleanup() {
        System.clearProperty(Constants.ENV_VAR_STARTUP_REQUIRED)
    }

    def "run sailor successfully"() {
        setup:
        blockingVar = new BlockingVariable(5)
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'helloworldaction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty('ELASTICIO_USER_ID'),
                start     : System.currentTimeMillis()
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

        byte[] payload = cipher.encryptMessage(msg).getBytes();

        amqp.publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(amqp.publishChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.amqp.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def bodyString = new String(body, "UTF-8");
                def message = IntegrationSpec.this.cipher.decryptMessage(bodyString);
                IntegrationSpec.this.blockingVar.set(message);
            }
        }

        amqp.publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor()

        then:
        def result = blockingVar.get()
        result.headers.isEmpty()
        JSON.stringify(result.body) == '{"echo":{"message":"Just do it!"}}'
        sailor.amqp.cancelConsumer()
    }

    def "should execute startup/init successfully"() {
        setup:
        System.setProperty(Constants.ENV_VAR_STARTUP_REQUIRED, "1");
        blockingVar = new BlockingVariable(5)
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'startupInitAction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty('ELASTICIO_USER_ID'),
                start     : System.currentTimeMillis()
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

        byte[] payload = cipher.encryptMessage(msg).getBytes();

        amqp.publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(amqp.publishChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.amqp.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def bodyString = new String(body, "UTF-8");
                def message = IntegrationSpec.this.cipher.decryptMessage(bodyString);
                IntegrationSpec.this.blockingVar.set(message);
            }
        }

        amqp.publishChannel.basicConsume(dataQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor()

        then:
        def result = blockingVar.get()
        result.headers.isEmpty()
        JSON.stringify(result.body) == '{"echo":{"message":"Show me startup/init"},"startupAndInit":{"startup":{"apiKey":"secret"},"init":{"apiKey":"secret"}}}'
        sailor.amqp.cancelConsumer()
    }

    def "should send http reply successfully"() {
        setup:

        blockingVar = new BlockingVariable(5)
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'httpReplyAction')

        def replyQueueName = prefix + 'request_reply_queue'
        def replyQueueRoutingKey = prefix + 'request_reply_routing_key'

        amqp.publishChannel.queueDeclare(replyQueueName, true, false, false, [:])
        amqp.publishChannel.queueBind(
                replyQueueName,
                System.getProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO),
                replyQueueRoutingKey)


        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty('ELASTICIO_USER_ID'),
                start     : System.currentTimeMillis(),
                'reply_to': replyQueueRoutingKey
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

        byte[] payload = cipher.encryptMessage(msg).getBytes();

        amqp.publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(amqp.publishChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.amqp.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def bodyString = new String(body, "UTF-8");
                def message = IntegrationSpec.this.cipher.decrypt(bodyString);
                IntegrationSpec.this.blockingVar.set(JSON.parseObject(message));
            }
        }

        amqp.publishChannel.basicConsume(replyQueueName, consumer)

        when:

        sailor = Sailor.createAndStartSailor()

        then:
        def result = blockingVar.get()
        result.statusCode.intValue() == HttpReply.Status.ACCEPTED.statusCode
        result.getJsonString('body').getString() == '{"echo":{"message":"Send me a reply"}}'
        JSON.stringify(result.get('headers')) == '{"Content-type":"application/json","x-custom-header":"abcdef"}'
        sailor.amqp.cancelConsumer()
    }

    def "publish init errors to RabbitMQ"() {
        setup:
        blockingVar = new BlockingVariable(5)
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'erroneousAction')

        def headers = [
                'execId'  : 'some-exec-id',
                'taskId'  : System.getProperty(Constants.ENV_VAR_FLOW_ID),
                'function': System.getProperty(Constants.ENV_VAR_FUNCTION),
                'userId'  : System.getProperty('ELASTICIO_USER_ID'),
                start     : System.currentTimeMillis()
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

        byte[] payload = cipher.encryptMessage(msg).getBytes();

        amqp.publishChannel.basicPublish(
                System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON),
                System.getProperty(Constants.ENV_VAR_DATA_ROUTING_KEY),
                options,
                payload);

        def consumer = new DefaultConsumer(amqp.publishChannel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body)
                    throws IOException {

                IntegrationSpec.this.amqp.publishChannel.basicAck(envelope.getDeliveryTag(), true)
                def errorJson = JSON.parseObject(new String(body, "UTF-8"))
                def error = IntegrationSpec.this.cipher.decrypt(errorJson.getString('error'));
                IntegrationSpec.this.blockingVar.set(error);
            }
        }

        amqp.publishChannel.basicConsume(errorsQueue, consumer)

        when:

        sailor = Sailor.createAndStartSailor()

        then:
        def result = blockingVar.get()
        println(result)
        def errorJson = JSON.parseObject(result);
        errorJson.getString('name') == 'java.lang.RuntimeException'
        errorJson.getString('message') == 'Ouch. Something went wrong'
        errorJson.getString('stack').startsWith('java.lang.RuntimeException: Ouch. Something went wrong')
    }
}
