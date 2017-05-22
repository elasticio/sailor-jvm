package io.elastic.sailor;


import javax.json.JsonObject;

public interface ApiClient {

    Step retrieveFlowStep(String taskId, String stepId);

    JsonObject updateAccount(String accountId, JsonObject body);

    void storeStartupState(String flowId, JsonObject body);

    JsonObject retrieveStartupState(String flowId);

    void deleteStartupState(String flowId);
}
