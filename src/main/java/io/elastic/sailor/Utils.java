package io.elastic.sailor;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final List<String> requiredSettings = new ArrayList<String>(){{
        add("AMQP_URI");
        add("LISTEN_MESSAGES_ON");
        add("PUBLISH_MESSAGES_TO");
        add("DATA_ROUTING_KEY");
        add("ERROR_ROUTING_KEY");
        add("REBOUND_ROUTING_KEY");
        add("TASK");
        add("STEP_ID");
    }};
    private static final Map<String, String> optionalSettings = new HashMap<String, String>(){{
        put("REBOUND_INITIAL_EXPIRATION", "15000");
        put("REBOUND_LIMIT", "20");
        put("COMPONENT_PATH", "");
    }};

    public static boolean isJsonObject(String input) {
        try {
            new Gson().fromJson(input, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    public static JsonObject parseToJson(String input) {
        return new JsonParser().parse(input).getAsJsonObject();
    }

    public static Map<String, String> validateSettings(Map<String, String> settings) {
        Map<String, String> result = new HashMap<>();

        logger.info("About to validate settings...");
        for (String each : requiredSettings) {
            if (settings.containsKey(each)) {
                result.put(each, settings.get(each));
                logger.info("Validated setting: " + each + " => " + settings.get(each));
            } else {
                throwError(each + " is missing");
            }
        }

        for (String each : optionalSettings.keySet()) {
            if (settings.containsKey(each)) {
                result.put(each, settings.get(each));
                logger.info("Validated setting: " + each + " => " + settings.get(each));
            } else {
                result.put(each, optionalSettings.get(each));
                logger.info("Validated setting: " + each + " => " + optionalSettings.get(each));
            }
        }

        return result;
    }

    private static void throwError(String message) {
        throw new IllegalArgumentException(message);
    }
}
