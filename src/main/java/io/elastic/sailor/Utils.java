package io.elastic.sailor;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;

class Utils {
    private static final Logger logger = LoggerFactory.getLogger(Utils.class.getName());

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
            auth(httpPost, url);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                throw new RuntimeException("Null response received");
            } else {
                String result = EntityUtils.toString(responseEntity);
                EntityUtils.consume(responseEntity);
                logger.info("Successfully posted json {} bytes length", body.toString().length());
                return result;
            }
        } finally {
            httpClient.close();
        }
    }

    private static void auth(final HttpRequest request, final String url) throws IOException {

        URL aURL = new URL(url);
        final String userInfo = aURL.getUserInfo();

        if (userInfo == null) {
            throw new IllegalArgumentException("User info is missing in the given url: " + url);
        }


        String decodedUserInfo = URLDecoder.decode(userInfo, "UTF-8");

        final String[] userAndPassword = decodedUserInfo.split(":");

        if (userAndPassword.length != 2) {
            throw new IllegalArgumentException("Either username or password is missing");
        }

        final UsernamePasswordCredentials credentials
                = new UsernamePasswordCredentials(userAndPassword[0], userAndPassword[1]);

        try {
            final Header header = new BasicScheme()
                    .authenticate(credentials, request, null);
            request.addHeader(header);
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }

    }

    public static String getEnvVar(final String key) {
        final String value = getOptionalEnvVar(key);

        if (value == null) {
            throw new IllegalStateException(
                    String.format("Env var '%s' is required", key));
        }

        return value;
    }

    public static String getOptionalEnvVar(final String key) {
        String value = System.getenv(key);

        if (value == null) {
            value = System.getProperty(key);
        }

        return value;
    }
}