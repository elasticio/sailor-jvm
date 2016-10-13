package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public interface ApiClient {

    Step retrieveTaskStep(String taskId, String stepId);

    JsonElement updateAccount(String accountId, JsonObject body);
}
