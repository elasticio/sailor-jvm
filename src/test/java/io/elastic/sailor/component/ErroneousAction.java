package io.elastic.sailor.component;


import io.elastic.api.ExecutionParameters;
import io.elastic.api.Function;

public class ErroneousAction implements Function {

    @Override
    public void execute(ExecutionParameters parameters) {
        throw new RuntimeException("Ouch. Something went wrong");
    }
}
