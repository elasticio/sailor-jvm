package io.elastic.sailor;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;

public final class Step {

    private final String id;
    private final String compId;
    private final String function;
    private final JsonObject cfg;
    private final String snapshotUri;
    private final boolean passThroughRequired;
    private final boolean putIncomingMessageIntoPassThrough;

    public Step(final JsonObject data, String snapshotUri) {
        this(data, snapshotUri, false);
    }

    public Step(final JsonObject data, String snapshotUri, final boolean putIncomingMessageIntoPassThrough) {
        this.id = getAsRequiredString(data, Constants.STEP_PROPERTY_ID);
        this.compId = getAsRequiredString(data, Constants.STEP_PROPERTY_COMP_ID);
        this.function = getAsRequiredString(data, Constants.STEP_PROPERTY_FUNCTION);
        this.cfg = getAsNullSafeObject(data, Constants.STEP_PROPERTY_CFG);
        this.snapshotUri = snapshotUri;
        this.passThroughRequired = data.getBoolean(Constants.STEP_PROPERTY_PASSTHROUGH, false);
        this.putIncomingMessageIntoPassThrough = putIncomingMessageIntoPassThrough;
    }

    public String getId() {
        return this.id;
    }

    public String getCompId() {
        return this.compId;
    }

    public String getFunction() {
        return this.function;
    }

    public JsonObject getCfg() {
        return this.cfg;
    }

    public String getSnapshotUri() {
        return this.snapshotUri;
    }

    public boolean isPassThroughRequired() {
        return passThroughRequired;
    }

    public boolean isPutIncomingMessageIntoPassThrough() {
        return putIncomingMessageIntoPassThrough;
    }

    private static String getAsRequiredString(
            final JsonObject data, final String name) {

        final JsonString value = data.getJsonString(name);

        if (value == null) {
            throw new IllegalArgumentException(
                    String.format("Step's %s is required", name));
        }

        return value.getString();
    }

    private static JsonObject getAsNullSafeObject(
            final JsonObject data, final String name) {

        final JsonObject value = data.getJsonObject(name);

        if (value != null) {
            return value;
        } else {
            return Json.createObjectBuilder().build();
        }
    }
}
