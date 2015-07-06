package io.elastic.sailor;

import java.util.Map;

public final class Parameters {
    private Map<String, String> params;

    public Parameters(Map<String, String> params) {
        this.params = params;
    }

    public String get(String key) {
        return params.get(key);
    }
}
