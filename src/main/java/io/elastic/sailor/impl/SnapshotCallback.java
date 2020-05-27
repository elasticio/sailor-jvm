package io.elastic.sailor.impl;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.name.Named;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ExecutionContext;
import io.elastic.sailor.MessagePublisher;

import javax.json.JsonObject;

public class SnapshotCallback extends CountingCallbackImpl {

    private ExecutionContext executionDetails;
    private MessagePublisher messagePublisher;
    private String routingKey;

    @Inject
    public SnapshotCallback(
            @Assisted ExecutionContext executionDetails,
            MessagePublisher messagePublisher,
            @Named(Constants.ENV_VAR_SNAPSHOT_ROUTING_KEY) String routingKey) {
        this.executionDetails = executionDetails;
        this.messagePublisher = messagePublisher;
        this.routingKey = routingKey;
    }

    @Override
    public void receiveData(Object data) {
        JsonObject snapshot = (JsonObject) data;

        byte[] payload = snapshot.toString().getBytes();

        messagePublisher.publish(routingKey, payload, executionDetails.buildAmqpProperties());
    }
}
