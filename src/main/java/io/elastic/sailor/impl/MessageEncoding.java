package io.elastic.sailor.impl;

public enum MessageEncoding {
    UTF8(2), BASE64(1);

    public final int protocolVersion;

    MessageEncoding(int protocolVersion) {
        this.protocolVersion = protocolVersion;
    }

    public static MessageEncoding fromProtocolVersion(final int version) {
        for (MessageEncoding e : values()) {
            if (e.protocolVersion == version) {
                return e;
            }
        }

        throw new IllegalArgumentException("Unknown protocol version");
    }

}
