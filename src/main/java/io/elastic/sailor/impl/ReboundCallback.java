package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;
import io.elastic.sailor.*;

import java.util.Map;

public class ReboundCallback extends CountingCallbackImpl {

    private static final String HEADER_REBOUND_REASON = "reboundReason";
    private static final String HEADER_REBOUND_ITERATION = "reboundIteration";

    private ExecutionContext executionContext;
    private MessagePublisher messagePublisher;
    private CryptoServiceImpl cipher;
    private Integer reboundLimit;
    private Integer reboundInitialExpiration;
    private String routingKey;

    @Inject
    public ReboundCallback(
            @Assisted ExecutionContext executionContext,
            MessagePublisher messagePublisher,
            CryptoServiceImpl cipher,
            @Named(Constants.ENV_VAR_REBOUND_LIMIT) Integer reboundLimit,
            @Named(Constants.ENV_VAR_REBOUND_INITIAL_EXPIRATION) Integer reboundInitialExpiration,
            @Named(Constants.ENV_VAR_REBOUND_ROUTING_KEY) String routingKey) {
        this.executionContext = executionContext;
        this.messagePublisher = messagePublisher;
        this.cipher = cipher;
        this.reboundLimit = reboundLimit;
        this.reboundInitialExpiration = reboundInitialExpiration;
        this.routingKey = routingKey;
    }

    public void receiveData(Object data) {

        int reboundIteration = getReboundIteration();

        if (reboundIteration > this.reboundLimit) {
            throw new RuntimeException("Rebound limit exceeded");
        }

        final Message message = executionContext.getMessage();
        byte[] payload = cipher.encryptMessage(message, MessageEncoding.BASE64);
        Map<String, Object> headers = executionContext.buildDefaultHeaders();
        headers.put(HEADER_REBOUND_REASON, data.toString());
        headers.put(HEADER_REBOUND_ITERATION, reboundIteration);

        final Integer expiration = getReboundExpiration(reboundIteration);
        messagePublisher.publish(routingKey, payload, makeReboundOptions(headers, expiration));
    }

    private int getReboundIteration() {
        final Map<String, Object> headers = executionContext.getHeaders();

        final Object reboundIteration = headers.get(HEADER_REBOUND_ITERATION);

        if (reboundIteration != null) {
            try {
                return Integer.parseInt(reboundIteration.toString()) + 1;
            } catch (Exception e) {
                throw new RuntimeException("Not a number in reboundIteration header: " + reboundIteration);
            }
        } else {
            return 1;
        }
    }

    protected Integer getReboundExpiration(int reboundIteration) {
        return Double.valueOf(Math.pow(2, reboundIteration - 1)).intValue()
                * this.reboundInitialExpiration;
    }

    protected AMQP.BasicProperties makeReboundOptions(Map<String, Object> headers, Integer expiration) {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .expiration(Integer.toString(expiration))
                .headers(headers)
                        //TODO: .mandatory(true)
                .build();
    }
}
