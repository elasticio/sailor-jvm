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
    private boolean isShutdownRequired;

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
        return this.isStartupRequired;
    }

    public boolean isShutdownRequired() {
        return this.isShutdownRequired;
    }

    @Inject
    public void setStartupRequired(@Named(Constants.ENV_VAR_STARTUP_REQUIRED)  boolean startupRequired) {
        this.isStartupRequired = startupRequired;
    }

    @Inject
    public void setShutdownRequired(@Named(Constants.ENV_VAR_SHUTDOWN_REQUIRED)  boolean isShutdownRequired) {
        this.isShutdownRequired = isShutdownRequired;
    }
}
