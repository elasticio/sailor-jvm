package io.elastic.sailor;

import java.util.Map;

public class AMQPWrapper {
    public static class Connection {

        private final Map<String, String> settings;

        public <T extends Map<String, String>> Connection(T settings) {
            this.settings = settings;
        }
    }
}
