package io.elastic.sailor.impl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.Function;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.json.JsonObject;
import java.io.IOException;
import java.nio.charset.Charset;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final CryptoServiceImpl cipher;
    private final MessageProcessor processor;
    private final Function function;
    private final Step step;
    private final ContainerContext containerContext;

    public MessageConsumer(Channel channel,
                           CryptoServiceImpl cipher,
                           MessageProcessor processor,
                           Function function,
                           Step step,
                           final ContainerContext containerContext) {
        super(channel);
        this.cipher = cipher;
        this.processor = processor;
        this.function = function;
        this.step = step;
        this.containerContext = containerContext;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        if (Sailor.gracefulShutdownHandler != null) {
            Sailor.gracefulShutdownHandler.increment();
        }

        ExecutionContext executionContext = null;
        long deliveryTag = envelope.getDeliveryTag();


        logger.info("Consumer {} received message: deliveryTag={}", consumerTag, deliveryTag);

        putIntoMDC(properties);

        try {
            executionContext = createExecutionContext(body, properties);
        } catch (Exception e) {
            logger.info("Failed to parse message to process {}", deliveryTag, e);
            this.getChannel().basicReject(deliveryTag, false);
            return;
        }

        ExecutionStats stats = null;

        try {
            stats = processor.processMessage(executionContext, this.function);
        } catch (Exception e) {
            logger.error("Failed to process message for delivery tag:" + deliveryTag, e);
        } finally {
            removeFromMDC(Constants.MDC_THREAD_ID);
            removeFromMDC(Constants.MDC_MESSAGE_ID);
            removeFromMDC(Constants.MDC_PARENT_MESSAGE_ID);
            ackOrReject(stats, deliveryTag);

            if (Sailor.gracefulShutdownHandler != null) {
                Sailor.gracefulShutdownHandler.decrement();
            }
        }
    }

    private void putIntoMDC(final AMQP.BasicProperties properties) {
        final String threadId = Utils.getThreadId(properties);
        final Object messageId = getHeaderValue(properties, Constants.AMQP_HEADER_MESSAGE_ID);
        final Object parentMessageId = getHeaderValue(properties, Constants.AMQP_HEADER_PARENT_MESSAGE_ID);

        MDC.put(Constants.MDC_THREAD_ID, threadId);
        MDC.put(Constants.MDC_MESSAGE_ID, messageId.toString());
        MDC.put(Constants.MDC_PARENT_MESSAGE_ID, parentMessageId.toString());

        logger.info("messageId={}, parentMessageId={}, threadId={}", messageId, parentMessageId, threadId);
    }

    private static void removeFromMDC(final String key) {
        try {
            MDC.remove(key);
        }catch(Exception e) {
            logger.warn("Failed to remove {} from MDC", key, e);
        }
    }

    private ExecutionContext createExecutionContext(final byte[] body, final AMQP.BasicProperties properties) {

        final String bodyString = new String(body, Charset.forName("UTF-8"));
        final JsonObject payload = cipher.decryptMessageContent(bodyString);

        final Message message = Utils.createMessage(payload);

        return new ExecutionContext(this.step, message, properties, this.containerContext);
    }


    private void ackOrReject(ExecutionStats stats, long deliveryTag) throws IOException {
        logger.info("Execution stats: {}", stats);

        if (stats == null || stats.getErrorCount() > 0) {
            logger.info("Reject received messages {}", deliveryTag);
            this.getChannel().basicReject(deliveryTag, false);

            return;
        }

        logger.info("Acknowledging received messages {}", deliveryTag);
        this.getChannel().basicAck(deliveryTag, true);
    }

    private Object getHeaderValue(final AMQP.BasicProperties properties, final String headerName) {
        return properties.getHeaders().getOrDefault(headerName, "unknown");
    }

}
