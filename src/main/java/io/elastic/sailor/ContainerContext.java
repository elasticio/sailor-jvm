package io.elastic.sailor;

import com.google.inject.Inject;
import com.google.inject.name.Named;

public class ContainerContext {

    private String flowId;
    private String stepId;
    private String execId;
    private String userId;
    private String compId;
    private String function;
    private boolean isStartupRequired;
    private String containerId;
    private String apiUserName;
    private String componentName;
    private String contractId;
    private String execType;
    private String execResultId;
    private String flowVersion;
    private String flowUserEmail;
    private String tenantId;
    private String workspaceId;

    public String getFlowId() {
        return flowId;
    }

    @Inject
    public void setFlowId(@Named(Constants.ENV_VAR_FLOW_ID) String flowId) {
        this.flowId = flowId;
    }

    public String getStepId() {
        return stepId;
    }

    @Inject
    public void setStepId(@Named(Constants.ENV_VAR_STEP_ID) String stepId) {
        this.stepId = stepId;
    }

    public String getExecId() {
        return execId;
    }

    @Inject
    public void setExecId(@Named(Constants.ENV_VAR_EXEC_ID) String execId) {
        this.execId = execId;
    }

    public String getUserId() {
        return userId;
    }

    @Inject
    public void setUserId(@Named(Constants.ENV_VAR_USER_ID) String userId) {
        this.userId = userId;
    }

    public String getCompId() {
        return compId;
    }

    @Inject
    public void setCompId(@Named(Constants.ENV_VAR_COMP_ID) String compId) {
        this.compId = compId;
    }

    public String getFunction() {
        return function;
    }

    @Inject
    public void setFunction(@Named(Constants.ENV_VAR_FUNCTION) String function) {
        this.function = function;
    }

    public boolean isStartupRequired() {
        return isStartupRequired;
    }

    @Inject
    public void setStartupRequired(@Named(Constants.ENV_VAR_STARTUP_REQUIRED)  boolean startupRequired) {
        isStartupRequired = startupRequired;
    }

    public String getContainerId() {
        return containerId;
    }

    @Inject
    public void setContainerId(@Named(Constants.ENV_VAR_CONTAINER_ID) String containerId) {
        this.containerId = containerId;
    }

    public String getApiUserName() {
        return apiUserName;
    }

    @Inject
    public void setApiUserName(@Named(Constants.ENV_VAR_API_USERNAME) String apiUserName) {
        this.apiUserName = apiUserName;
    }

    public String getComponentName() {
        return componentName;
    }

    @Inject(optional = true)
    public void setComponentName(@Named(Constants.ENV_VAR_COMP_NAME) String componentName) {
        this.componentName = componentName;
    }

    public String getContractId() {
        return contractId;
    }

    @Inject(optional = true)
    public void setContractId(@Named(Constants.ENV_VAR_CONTRACT_ID) String contractId) {
        this.contractId = contractId;
    }

    public String getExecType() {
        return execType;
    }

    @Inject(optional = true)
    public void setExecType(@Named(Constants.ENV_VAR_EXEC_TYPE) String execType) {
        this.execType = execType;
    }

    public String getExecResultId() {
        return execResultId;
    }

    @Inject(optional = true)
    public void setExecResultId(@Named(Constants.ENV_VAR_EXECUTION_RESULT_ID) String execResultId) {
        this.execResultId = execResultId;
    }

    public String getFlowVersion() {
        return flowVersion;
    }

    @Inject(optional = true)
    public void setFlowVersion(@Named(Constants.ENV_VAR_FLOW_VERSION) String flowVersion) {
        this.flowVersion = flowVersion;
    }

    public String getFlowUserEmail() {
        return flowUserEmail;
    }

    @Inject(optional = true)
    public void setFlowUserEmail(@Named(Constants.ENV_VAR_TASK_USER_EMAIL) String flowUserEmail) {
        this.flowUserEmail = flowUserEmail;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Inject(optional = true)
    public void setTenantId(@Named(Constants.ENV_VAR_TENANT_ID) String tenantId) {
        this.tenantId = tenantId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    @Inject(optional = true)
    public void setWorkspaceId(@Named(Constants.ENV_VAR_WORKSPACE_ID) String workspaceId) {
        this.workspaceId = workspaceId;
    }


    @Override
    public String toString() {
        return "ContainerContext{" +
                "flowId='" + flowId + '\'' +
                ", stepId='" + stepId + '\'' +
                ", execId='" + execId + '\'' +
                ", userId='" + userId + '\'' +
                ", compId='" + compId + '\'' +
                ", function='" + function + '\'' +
                ", isStartupRequired=" + isStartupRequired +
                ", containerId='" + containerId + '\'' +
                ", apiUserName='" + apiUserName + '\'' +
                ", componentName='" + componentName + '\'' +
                ", contractId='" + contractId + '\'' +
                ", execType='" + execType + '\'' +
                ", execResultId='" + execResultId + '\'' +
                ", flowVersion='" + flowVersion + '\'' +
                ", flowUserEmail='" + flowUserEmail + '\'' +
                ", tenantId='" + tenantId + '\'' +
                ", workspaceId='" + workspaceId + '\'' +
                '}';
    }
}
