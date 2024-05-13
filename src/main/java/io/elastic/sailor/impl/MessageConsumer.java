package io.elastic.sailor.impl;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import io.elastic.api.Function;
import io.elastic.api.Message;
import io.elastic.sailor.*;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import io.elastic.sailor.impl.HttpUtils.BasicAuthorizationHandler;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private final CryptoServiceImpl cipher;
    private final MessageProcessor processor;
    private final Function function;
    private final Step step;
    private final ContainerContext containerContext;
    private final MessageResolver messageResolver;
    private final Channel channel;
    private final ExecutorService threadPool;
    private BasicAuthorizationHandler authorizationHandler;

    public MessageConsumer(Channel channel,
                           CryptoServiceImpl cipher,
                           MessageProcessor processor,
                           Function function,
                           Step step,
                           final ContainerContext containerContext,
                           final MessageResolver messageResolver,
                           ExecutorService threadPool) {
        super(channel);
        this.channel = channel;
        this.cipher = cipher;
        this.processor = processor;
        this.function = function;
        this.step = step;
        this.containerContext = containerContext;
        this.messageResolver = messageResolver;
        this.threadPool = threadPool;
    }

    @Override
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               final byte[] body) throws IOException {
            threadPool.submit(() -> {
                if (Sailor.gracefulShutdownHandler != null) {
                    Sailor.gracefulShutdownHandler.increment();
                }
                ExecutionContext executionContext = null;
                long deliveryTag = envelope.getDeliveryTag();
                putIntoMDC(properties);

                try {
                    executionContext = createExecutionContext(body, properties);
                } catch (Exception e) {
                    try {
                        channel.basicReject(deliveryTag, false);
                    } catch (IOException ioException) {
                        logger.error("Failed to basicReject message: {}", Utils.getStackTrace(e));
                    }
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
                    try {
                        ackOrReject(stats, deliveryTag);
                    } catch (IOException e) {
                        logger.error("Failed to ackOrReject message: {}", Utils.getStackTrace(e));
                    }
                    decrement();
                }
            });
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
        final String uri = this.step.getSnapshotUri();
        logger.info("Retrieving step data at: {}", uri);
        final JsonObject step = HttpUtils.getJson(uri, authorizationHandler, 4);
        final JsonObject snapshot = getAsNullSafeObject(step, Constants.STEP_PROPERTY_SNAPSHOT);
        final Message message = messageResolver.materialize(body, properties);
        return new ExecutionContext(this.step, body, message, properties, this.containerContext, snapshot);
    }


    private JsonObject getAsNullSafeObject(
            final JsonObject data, final String name) {

        final JsonObject value = data.getJsonObject(name);

        if (value != null) {
            return value;
        } else {
            return Json.createObjectBuilder().build();
        }
    }

    private void ackOrReject(ExecutionStats stats, long deliveryTag) throws IOException {
        logger.info("Execution stats: {}", stats);

        if (stats == null || stats.getErrorCount() > 0) {
            logger.info("Reject received messages {}", deliveryTag);
            this.getChannel().basicReject(deliveryTag, false);

            return;
        }

        logger.info("Acknowledging received message with deliveryTag={}", deliveryTag);
        this.getChannel().basicAck(deliveryTag, false);
    }

    private Object getHeaderValue(final AMQP.BasicProperties properties, final String headerName) {
        return properties.getHeaders().getOrDefault(headerName, "unknown");
    }

    /**
     * Called when consumer is registered.
     */
    public void handleConsumeOk(String consumerTag) {
        logger.debug("Consumer {} is registered", consumerTag);
    }

}
