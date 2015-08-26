package io.elastic.sailor;

import com.google.gson.JsonObject;
import io.elastic.api.EventEmitter;

public class SnapshotCallback implements EventEmitter.Callback {

    private ExecutionContext executionDetails;
    private AMQPWrapperInterface amqp;

    public SnapshotCallback(ExecutionContext executionDetails, AMQPWrapperInterface amqp) {
        this.executionDetails = executionDetails;
        this.amqp = amqp;
    }

    @Override
    public void receive(Object data) {
        JsonObject snapshot = (JsonObject) data;

        byte[] payload = snapshot.toString().getBytes();

        amqp.sendSnapshot(payload, executionDetails.buildDefaultOptions());
    }
}
