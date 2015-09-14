package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import com.rabbitmq.client.AMQP;
import io.elastic.api.Message;
import io.elastic.sailor.AMQPWrapperInterface;
import io.elastic.sailor.CipherWrapper;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ExecutionContext;
import io.elastic.sailor.impl.CountingCallbackImpl;

import java.util.Map;

public class ReboundCallback extends CountingCallbackImpl {

    private ExecutionContext executionContext;
    private AMQPWrapperInterface amqp;
    private CipherWrapper cipher;
    private Integer reboundLimit;
    private Integer reboundInitialExpiration;

    @Inject
    public ReboundCallback(
            @Assisted ExecutionContext executionContext,
            AMQPWrapperInterface amqp,
            CipherWrapper cipher,
            @Named(Constants.ENV_VAR_REBOUND_LIMIT) Integer reboundLimit,
            @Named(Constants.ENV_VAR_REBOUND_INITIAL_EXPIRATION) Integer reboundInitialExpiration) {
        this.executionContext = executionContext;
        this.amqp = amqp;
        this.cipher = cipher;
        this.reboundLimit = reboundLimit;
        this.reboundInitialExpiration = reboundInitialExpiration;
    }

    public void receiveData(Object data) {

        int reboundIteration = getReboundIteration();

        if (reboundIteration > this.reboundLimit) {
            throw new RuntimeException("Rebound limit exceeded");
        }

        final Message message = executionContext.getMessage();
        byte[] payload = cipher.encryptMessage(message).getBytes();
        Map<String, Object> headers = executionContext.buildDefaultHeaders();
        headers.put("reboundReason", data.toString());
        headers.put("reboundIteration", reboundIteration);
        double expiration = getReboundExpiration(reboundIteration);
        amqp.sendRebound(payload, makeReboundOptions(headers, expiration));
    }

    private int getReboundIteration() {
        final Map<String, Object> headers = executionContext.getHeaders();

        final Object reboundIteration = headers.get("reboundIteration");

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

    private double getReboundExpiration(int reboundIteration) {
        return Math.pow(2, reboundIteration - 1) * this.reboundInitialExpiration;
    }

    private AMQP.BasicProperties makeReboundOptions(Map<String, Object> headers, double expiration) {
        return new AMQP.BasicProperties.Builder()
                .contentType("application/json")
                .contentEncoding("utf8")
                .expiration(Double.toString(expiration))
                .headers(headers)
                        //TODO: .mandatory(true)
                .build();
    }
}
