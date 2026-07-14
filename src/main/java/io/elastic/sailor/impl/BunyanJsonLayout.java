package io.elastic.sailor.impl;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.LayoutBase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.elastic.sailor.Constants;
import io.elastic.sailor.ContainerContext;
import org.slf4j.MDC;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class BunyanJsonLayout extends LayoutBase<ILoggingEvent> {

    public static final String LEVEL_STRING = "level_str";
    public static final String THREAD_ID = "threadId";
    public static final String MESSAGE_ID = "messageId";
    public static final String PARENT_MESSAGE_ID = "parentMessageId";

    // Required fields for Bunyan format
    public static final String VERSION = "v";
    public static final String LEVEL = "level";
    public static final String NAME = "name";
    public static final String HOSTNAME = "hostname";
    public static final String PID = "pid";
    public static final String TIME = "time";
    public static final String MESSAGE = "msg";

    private static int BUNYAN_LEVEL_TRACE = 10;
    private static int BUNYAN_LEVEL_DEBUG = 20;
    private static int BUNYAN_LEVEL_INFO = 30;
    private static int BUNYAN_LEVEL_WARN = 40;
    private static int BUNYAN_LEVEL_ERROR = 50;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static ContainerContext containerContext;

    private boolean appendLineSeparator = true;

    public void setAppendLineSeparator(boolean appendLineSeparator) {
        this.appendLineSeparator = appendLineSeparator;
    }

    @Override
    public String doLayout(ILoggingEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();

        map.put(LEVEL, getBunyanLevel(event));
        map.put("thread", event.getThreadName());
        map.put("logger", event.getLoggerName());
        map.put("context", event.getLoggerContextVO().getName());

        putFromContainerContext(map);

        final String threadId = MDC.get(Constants.MDC_THREAD_ID);
        final String messageId = MDC.get(Constants.MDC_MESSAGE_ID);
        final String parentMessageId = MDC.get(Constants.MDC_PARENT_MESSAGE_ID);

        if (threadId != null) {
            map.put(THREAD_ID, threadId);
        }

        if (messageId != null) {
            map.put(MESSAGE_ID, messageId);
        }

        if (parentMessageId != null) {
            map.put(PARENT_MESSAGE_ID, parentMessageId);
        }

        map.put(VERSION, "0");
        map.put(NAME, "sailor-jvm");
        map.put(PID, getProcessId());
        map.put(TIME, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(new Date()));
        map.put(LEVEL_STRING, event.getLevel().levelStr);
        map.put(MESSAGE, event.getFormattedMessage());

        try {
            map.put(HOSTNAME, InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            map.put(HOSTNAME, "unknown-host");
        }

        try {
            String json = objectMapper.writeValueAsString(map);
            return appendLineSeparator ? json + "\n" : json;
        } catch (JsonProcessingException e) {
            addError("Failed to serialize log event to JSON", e);
            return event.getFormattedMessage() + "\n";
        }
    }

    private String getProcessId() {
        String vmName = ManagementFactory.getRuntimeMXBean().getName();
        int p = vmName.indexOf("@");
        return vmName.substring(0, p);
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

    public int getBunyanLevel(final ILoggingEvent event) {
        final Level level = event.getLevel();

        if (level == Level.TRACE) {
            return BUNYAN_LEVEL_TRACE;
        } else if (level == Level.DEBUG) {
            return BUNYAN_LEVEL_DEBUG;
        } else if (level == Level.INFO) {
            return BUNYAN_LEVEL_INFO;
        } else if (level == Level.WARN) {
            return BUNYAN_LEVEL_WARN;
        } else if (level == Level.ERROR) {
            return BUNYAN_LEVEL_ERROR;
        }

        return BUNYAN_LEVEL_INFO;
    }
}