package io.elastic.sailor.impl;

import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import io.elastic.api.Message;
import io.elastic.api.Module;
import io.elastic.sailor.*;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.json.JsonObject;
import java.io.IOException;
import java.nio.charset.Charset;

public class MessageConsumer extends DefaultConsumer {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MessageConsumer.class);
    private static final String MDC_TRACE_ID = "traceId";
    private final CryptoServiceImpl cipher;
    private final MessageProcessor processor;
    private final Module module;
    private final Step step;

    public MessageConsumer(Channel channel,
                           CryptoServiceImpl cipher,
                           MessageProcessor processor,
                           Module module,
                           @Named(Constants.NAME_STEP_JSON) Step step) {
        super(channel);
        this.cipher = cipher;
        this.processor = processor;
        this.module = module;
        this.step = step;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
            throws IOException {

        ExecutionContext executionContext = null;
        long deliveryTag = envelope.getDeliveryTag();

        final Object messageId = getHeaderValue(properties, Constants.AMQP_HEADER_MESSAGE_ID);
        final Object parentMessageId = getHeaderValue(properties, Constants.AMQP_HEADER_PARENT_MESSAGE_ID);
        final Object traceId = getHeaderValue(properties, Constants.AMQP_META_HEADER_TRACE_ID);

        if (traceId != null) {
            MDC.put(MDC_TRACE_ID, traceId.toString());
        }

        logger.info("Consumer {} received message: deliveryTag={}, messageId={}, parentMessageId={}, traceId={}",
                consumerTag, deliveryTag, messageId, parentMessageId, traceId);

        try {
            executionContext = createExecutionContext(body, properties);
        } catch (Exception e) {
            logger.info("Failed to parse message to process {}", deliveryTag, e);
            this.getChannel().basicReject(deliveryTag, false);
            return;
        }

        ExecutionStats stats = null;

        try {
            stats = processor.processMessage(executionContext, this.module);
        } catch (Exception e) {
            logger.error("Failed to process message for delivery tag:" + deliveryTag, e);
        } finally {
            try {
                MDC.remove(MDC_TRACE_ID);
            }catch(Exception e) {
                logger.warn("Failed to remove {} from MDC", MDC_TRACE_ID, e);
            }
            ackOrReject(stats, deliveryTag);
        }
    }

    private ExecutionContext createExecutionContext(final byte[] body, final AMQP.BasicProperties properties) {

        final String bodyString = new String(body, Charset.forName("UTF-8"));
        final JsonObject payload = cipher.decryptMessageContent(bodyString);

        final Message message = Utils.createMessage(payload);

        final JsonObject passthrough = payload.getJsonObject(Constants.MESSAGE_PROPERTY_PASSTHROUGH);

        return new ExecutionContext(this.step, message, properties, passthrough);
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
