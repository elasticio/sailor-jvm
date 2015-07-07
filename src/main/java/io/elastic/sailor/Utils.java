package io.elastic.sailor;

import com.google.gson.*;

public class Utils {

    public static boolean isJsonObject(String input) {
        try {
            new Gson().fromJson(input, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    public static JsonObject toJson(String input) {
        return new JsonParser().parse(input).getAsJsonObject();
    }
}