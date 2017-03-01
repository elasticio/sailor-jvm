package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.AmqpService;
import io.elastic.sailor.ExecutionContext;

import javax.json.JsonObject;

public class SnapshotCallback extends CountingCallbackImpl {

    private ExecutionContext executionDetails;
    private AmqpService amqp;

    @Inject
    public SnapshotCallback(
            @Assisted ExecutionContext executionDetails,
            AmqpService amqp) {
        this.executionDetails = executionDetails;
        this.amqp = amqp;
    }

    @Override
    public void receiveData(Object data) {
        JsonObject snapshot = (JsonObject) data;

        byte[] payload = snapshot.toString().getBytes();

        amqp.sendSnapshot(payload, executionDetails.buildAmqpProperties());
    }
}
