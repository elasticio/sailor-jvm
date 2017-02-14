package io.elastic.sailor.component;


import io.elastic.api.Component;
import io.elastic.api.EventEmitter;
import io.elastic.api.ExecutionParameters;

public class ErroneousAction implements Component {

    @Override
    public void execute(ExecutionParameters parameters) {
        throw new RuntimeException("Ouch. Something went wrong");
    }
}
