package io.elastic.sailor;

public class ExecutionStats {
    private int dataCount;
    private int errorCount;
    private int reboundCount;

    public ExecutionStats(int dataCount, int errorCount, int reboundCount) {
        this.dataCount = dataCount;
        this.errorCount = errorCount;
        this.reboundCount = reboundCount;
    }

    public int getDataCount() {
        return dataCount;
    }

    public int getErrorCount() {
        return errorCount;
    }

    public int getReboundCount() {
        return reboundCount;
    }

    @Override
    public String toString() {
        return "ExecutionStats{" +
                "dataCount=" + dataCount +
                ", errorCount=" + errorCount +
                ", reboundCount=" + reboundCount +
                '}';
    }
}
