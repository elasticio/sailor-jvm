package io.elastic.sailor;

public class UnexpectedStatusCodeException extends RuntimeException {

    private final int statusCode;

    public UnexpectedStatusCodeException(final int statusCode) {
        super(String.format("Got %s response", statusCode));
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return this.statusCode;
    }
}
