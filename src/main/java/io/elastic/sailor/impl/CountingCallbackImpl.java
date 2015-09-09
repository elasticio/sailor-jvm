package io.elastic.sailor.impl;

import io.elastic.sailor.CountingCallback;

public abstract class CountingCallbackImpl implements CountingCallback {

    private int count;

    public abstract void receiveData(Object data);

    @Override
    public void receive(Object data) {
        receiveData(data);
        this.count++;
    }

    @Override
    public int getCount() {
        return count;
    }
}
