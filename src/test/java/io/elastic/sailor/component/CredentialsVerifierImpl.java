package io.elastic.sailor.component;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.elastic.api.CredentialsVerifier;
import io.elastic.api.InvalidCredentialsException;

public class CredentialsVerifierImpl implements CredentialsVerifier {

    public static String VERIFICATION_RESULT_CFG_KEY = "verification-result";

    @Override
    public void verify(JsonObject configuration) throws InvalidCredentialsException {

        final JsonElement result = configuration.get(VERIFICATION_RESULT_CFG_KEY);

        if (result != null && !result.getAsBoolean()) {
            throw new InvalidCredentialsException("Invalid credentials");
        }
    }
}