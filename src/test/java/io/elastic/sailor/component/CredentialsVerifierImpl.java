package io.elastic.sailor.component;

import com.google.gson.JsonObject;
import io.elastic.api.CredentialsVerifier;
import io.elastic.api.InvalidCredentialsException;

public class CredentialsVerifierImpl implements CredentialsVerifier {
    public static boolean success = true;

    @Override
    public void verify(JsonObject configuration) throws InvalidCredentialsException {
        if (!success) throw new InvalidCredentialsException("Invalid credentials");
        // always success until flag changed
    }
}