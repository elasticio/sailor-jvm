package io.elastic.sailor.impl;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.contrib.json.classic.JsonLayout;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ContainerContext;
import org.slf4j.MDC;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class BunyanJsonLayout extends JsonLayout {

    public static final String TIME = "time";
    public static final String HOSTNAME = "hostname";
    public static final String THREAD_ID = "threadId";
    public static final String MESSAGE_ID = "messageId";
    public static final String PARENT_MESSAGE_ID = "parentMessageId";
    public static final String MESSAGE = "msg";

    public static ContainerContext containerContext;

    @Override
    protected void addCustomDataToJsonMap(Map<String, Object> map, ILoggingEvent event) {
        super.addCustomDataToJsonMap(map, event);

        final String time = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date());
        final String threadId = MDC.get(Constants.MDC_THREAD_ID);
        final String messageId = MDC.get(Constants.MDC_MESSAGE_ID);
        final String parentMessageId = MDC.get(Constants.MDC_PARENT_MESSAGE_ID);

        putFromContainerContext(map);

        if (threadId != null) {
            map.put(THREAD_ID, threadId);
        }

        if (messageId != null) {
            map.put(MESSAGE_ID, messageId);
        }

        if (parentMessageId != null) {
            map.put(PARENT_MESSAGE_ID, parentMessageId);
        }

        map.put(BunyanJsonLayout.TIME, time);
        map.remove(JsonLayout.TIMESTAMP_ATTR_NAME);

        final Object message = map.get(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME);
        map.put(MESSAGE, message);
        map.remove(JsonLayout.FORMATTED_MESSAGE_ATTR_NAME);

        try {
            map.put(BunyanJsonLayout.HOSTNAME, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            // ignore
        }
    }

    private void putFromContainerContext(final Map<String, Object> map) {

        if (BunyanJsonLayout.containerContext == null) {
            return;
        }

        map.put(Constants.ENV_VAR_CONTAINER_ID, BunyanJsonLayout.containerContext.getContainerId());
        map.put(Constants.ENV_VAR_API_USERNAME, BunyanJsonLayout.containerContext.getApiUserName());
        map.put(Constants.ENV_VAR_COMP_NAME, BunyanJsonLayout.containerContext.getComponentName());
        map.put(Constants.ENV_VAR_CONTRACT_ID, BunyanJsonLayout.containerContext.getContractId());
        map.put(Constants.ENV_VAR_EXEC_TYPE, BunyanJsonLayout.containerContext.getExecType());
        map.put(Constants.ENV_VAR_EXECUTION_RESULT_ID, BunyanJsonLayout.containerContext.getExecResultId());
        map.put(Constants.ENV_VAR_FLOW_VERSION, BunyanJsonLayout.containerContext.getFlowVersion());
        map.put(Constants.ENV_VAR_TASK_USER_EMAIL, BunyanJsonLayout.containerContext.getFlowUserEmail());
        map.put(Constants.ENV_VAR_TENANT_ID, BunyanJsonLayout.containerContext.getTenantId());
        map.put(Constants.ENV_VAR_WORKSPACE_ID, BunyanJsonLayout.containerContext.getWorkspaceId());
    }
}
