package io.elastic.sailor.component;

import io.elastic.api.CredentialsVerifier;
import io.elastic.api.InvalidCredentialsException;
import net.minidev.json.JSONValue;

import jakarta.json.JsonObject;
import jakarta.json.JsonValue;

public class CredentialsVerifierImpl implements CredentialsVerifier {

    public static String VERIFICATION_RESULT_CFG_KEY = "verification-result";

    @Override
    public void verify(JsonObject configuration) throws InvalidCredentialsException {

        final JsonValue value = configuration.get(VERIFICATION_RESULT_CFG_KEY);

        if (value == JsonValue.FALSE) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
}