package io.elastic.sailor.component;


import io.elastic.api.ExecutionParameters;
import io.elastic.api.Module;

public class ErroneousAction implements Module {

    @Override
    public void execute(ExecutionParameters parameters) {
        throw new RuntimeException("Ouch. Something went wrong");
    }
}
