package io.elastic.sailor;

import io.elastic.api.Message;

import java.util.Map;

public interface MessageProcessor {

    void processMessage(final Message incomingMessage,
                        final Map<String, Object> incomingHeaders,
                        final Long deliveryTag);
}
