package io.elastic.sailor.impl;

import com.rabbitmq.client.*;
import io.elastic.api.Function;
import io.elastic.api.Message;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;

public class MessageConsumer implements DeliverCallback {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final Channel channel;
    private final MessageProcessor processor;
    private final Function function;
    private final Step step;
    private final ContainerContext containerContext;
    private final MessageResolver messageResolver;

    public MessageConsumer(final Channel channel,
                           final MessageProcessor processor,
                           final Function function,
                           final Step step,
                           final ContainerContext containerContext,
                           final MessageResolver messageResolver) {
        this.channel = channel;
        this.processor = processor;
        this.function = function;
        this.step = step;
        this.containerContext = containerContext;
        this.messageResolver = messageResolver;
    }

    @Override
    public void handle(String consumerTag, Delivery message) throws IOException {
        final Envelope envelope = message.getEnvelope();
        final AMQP.BasicProperties properties = message.getProperties();

        if (Sailor.gracefulShutdownHandler != null) {
            Sailor.gracefulShutdownHandler.increment();
        }

        ExecutionContext executionContext = null;
        long deliveryTag = envelope.getDeliveryTag();


        logger.info("Consumer {} received message: deliveryTag={}", consumerTag, deliveryTag);

        putIntoMDC(properties);

        try {
            executionContext = createExecutionContext(message.getBody(), properties);
        } catch (Exception e) {
            this.channel.basicReject(deliveryTag, false);
            logger.error("Failed to parse or resolve message to process {}", Utils.getStackTrace(e));

            decrement();

            return;
        }

        ExecutionStats stats = null;

        try {
            stats = processor.processMessage(executionContext, this.function);
        } catch (Exception e) {
            logger.error("Failed to process message: {}", Utils.getStackTrace(e));
        } finally {
            removeFromMDC(Constants.MDC_THREAD_ID);
            removeFromMDC(Constants.MDC_MESSAGE_ID);
            removeFromMDC(Constants.MDC_PARENT_MESSAGE_ID);
            ackOrReject(stats, deliveryTag);

            decrement();
        }
    }

    private void decrement() {
        try {
            if (Sailor.gracefulShutdownHandler != null) {
                Sailor.gracefulShutdownHandler.decrement();
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
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
        } catch (Exception e) {
            logger.warn("Failed to remove {} from MDC: {}", key, Utils.getStackTrace(e));
        }
    }

    private ExecutionContext createExecutionContext(final byte[] body, final AMQP.BasicProperties properties) {

        final Message message = messageResolver.materialize(body, properties);

        return new ExecutionContext(this.step, body, message, properties, this.containerContext);
    }


    private void ackOrReject(ExecutionStats stats, long deliveryTag) throws IOException {
        logger.info("Execution stats: {}", stats);

        if (stats == null || stats.getErrorCount() > 0) {
            logger.info("Reject received messages {}", deliveryTag);
            this.channel.basicReject(deliveryTag, false);

            return;
        }

        logger.info("Acknowledging received message with deliveryTag={}", deliveryTag);
        this.channel.basicAck(deliveryTag, true);
    }

    private Object getHeaderValue(final AMQP.BasicProperties properties, final String headerName) {
        return properties.getHeaders().getOrDefault(headerName, "unknown");
    }
}
