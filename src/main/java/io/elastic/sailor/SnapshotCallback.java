package io.elastic.sailor;

import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import io.elastic.sailor.impl.CountingCallbackImpl;

public class SnapshotCallback extends CountingCallbackImpl {

    private ExecutionContext executionDetails;
    private AMQPWrapperInterface amqp;

    @Inject
    public SnapshotCallback(
            @Assisted ExecutionContext executionDetails,
            AMQPWrapperInterface amqp) {
        this.executionDetails = executionDetails;
        this.amqp = amqp;
    }

    @Override
    public void receiveData(Object data) {
        JsonObject snapshot = (JsonObject) data;

        byte[] payload = snapshot.toString().getBytes();

        amqp.sendSnapshot(payload, executionDetails.buildDefaultOptions());
    }
}
