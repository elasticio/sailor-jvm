package io.elastic.sailor

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.DefaultConsumer
import com.rabbitmq.client.Envelope
import io.elastic.api.JSON
import io.elastic.api.Message
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler
import spock.lang.Shared
import spock.lang.Specification
import spock.util.concurrent.BlockingVariable

import javax.json.Json
import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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
    def blockingVar = new BlockingVariable<Message>(5)

    def setupSpec() {
        System.setProperty(Constants.ENV_VAR_API_URI, 'http://localhost:8182')
        System.setProperty(Constants.ENV_VAR_API_USERNAME, 'test@test.com')
        System.setProperty(Constants.ENV_VAR_API_KEY, '5559edd')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD, 'testCryptoPassword')
        System.setProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV, 'iv=any16_symbols')
        System.setProperty(Constants.ENV_VAR_FLOW_ID, '5559edd38968ec0736000003')
        System.setProperty(Constants.ENV_VAR_STEP_ID, 'step_1')
        System.setProperty(Constants.ENV_VAR_FUNCTION, 'helloworldaction')
        System.setProperty('ELASTICIO_USER_ID', '5559edd38968ec0736000002')
        System.setProperty('ELASTICIO_COMP_ID', '5559edd38968ec0736000456')

        System.setProperty(Constants.ENV_VAR_AMQP_URI, 'amqp://guest:guest@localhost:5672')
        System.setProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON, prefix + ':messages')
        System.setProperty(Constants.ENV_VAR_PUBLISH_MESSAGES_TO, prefix + ':exchange')
        System.setProperty(Constants.ENV_VAR_DATA_ROUTING_KEY, prefix + ':routing_key:message')
        System.setProperty(Constants.ENV_VAR_ERROR_ROUTING_KEY, prefix + ':routing_key:error')
        System.setProperty(Constants.ENV_VAR_REBOUND_ROUTING_KEY, prefix + ':routing_key:rebound')
        System.setProperty(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY, prefix + ':routing_key:snapshot')

        cipher = new CipherWrapper(
                System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_PASSWORD),
                System.getProperty(Constants.ENV_VAR_MESSAGE_CRYPTO_IV))

        amqp = new AMQPWrapper(cipher)

        amqp.setAmqpUri(System.getProperty(Constants.ENV_VAR_AMQP_URI))
        amqp.setPublishExchangeName(System.getProperty(Constants.ENV_VAR_LISTEN_MESSAGES_ON))
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
                        .add(Constants.STEP_PROPERTY_COMP_ID, System.getProperty('ELASTICIO_COMP_ID'))
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

    def "run sailor successfully"() {
        setup:

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

        Sailor.main();

        then:
        def result = blockingVar.get()
        result.headers.isEmpty()
        JSON.stringify(result.body) == '{"message":"Just do it!"}'
    }
}
