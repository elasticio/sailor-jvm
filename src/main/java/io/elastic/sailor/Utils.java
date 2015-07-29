package io.elastic.sailor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class Utils {
    private static final Logger logger = Logger.getLogger(Utils.class.getName());

    public static boolean isJsonObject(String input) {
        try {
            new Gson().fromJson(input, Object.class);
            return true;
        } catch (JsonSyntaxException e) {
            return false;
        }
    }

    public static boolean isJsonObject(JsonElement element) {
        return element != null && element.isJsonObject();
    }

    public static String postJson(String url, JsonObject body) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader("Content-Type", "application/json");
        httpPost.setEntity(new StringEntity(body.toString()));
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                throw new RuntimeException("Null response received");
            } else {
                String result = EntityUtils.toString(responseEntity);
                EntityUtils.consume(responseEntity);
                logger.log(Level.INFO, "Successfully posted json " + body.toString().length() + " bytes length");
                return result;
            }
        } finally {
            httpClient.close();
        }
    }
}