package io.elastic.sailor;

import io.elastic.api.Message;
import io.elastic.api.Module;

import java.util.Map;

public interface MessageProcessor {

    ExecutionStats processMessage(final Message incomingMessage,
                        final Map<String, Object> incomingHeaders,
                        final Module module);
}
