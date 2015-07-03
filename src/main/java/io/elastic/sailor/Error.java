package io.elastic.sailor;

class Error {
    public String name;
    public String message;
    public String stack;

    public Error(String name, String message, String stack) {
        this.name = name;
        this.message = message;
        this.stack = stack;
    }
}
