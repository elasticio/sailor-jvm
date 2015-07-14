package io.elastic.sailor;

public class Error {

    public final String name;
    public final String message;
    public final String stack;

    public Error(String name, String message, String stack) {
        this.name = name;
        this.message = message;
        this.stack = stack;
    }

    public Error(Throwable e) {
        this.name = "Error";
        this.message = e.getMessage();
        this.stack = getStack(e);
    }

    private String getStack(Throwable e){
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append(element.toString());
            sb.append("\n");
        }
        return sb.toString();
    }
}
