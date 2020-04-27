package io.elastic.sailor;

import io.elastic.api.Function;

public interface MessageProcessor {

    ExecutionStats processMessage(final ExecutionContext executionContext, final Function function);
}
