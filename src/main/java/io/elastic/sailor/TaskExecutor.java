package io.elastic.sailor;

public class TaskExecutor {
    private static final int TIMEOUT = System.getenv("TIMEOUT")!=null && Integer.parseInt(System.getenv("TIMEOUT"))>0 ?
            Integer.parseInt(System.getenv("TIMEOUT")) : 20 * 60 * 1000;

    public TaskExecutor() {

    }
}
