package io.elastic.sailor;

import io.elastic.api.EventEmitter;

public interface CountingCallback extends EventEmitter.Callback {

    int getCount();
}
