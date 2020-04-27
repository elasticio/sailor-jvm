package io.elastic.api.demo;

import io.elastic.api.Function;
import io.elastic.api.ExecutionParameters;

public class ErroneousComponent implements Function {

    @Override
    public void execute(ExecutionParameters parameters) {

        throw new RuntimeException("Ouch! We did not expect that");
    }
}
