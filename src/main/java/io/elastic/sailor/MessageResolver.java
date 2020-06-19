package io.elastic.sailor;

import io.elastic.api.Message;

public interface MessageResolver {

    Message resolve(final byte[] body);
}
