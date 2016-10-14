package io.elastic.sailor;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthenticationException;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class.getName());

    public static String postJson(String url, JsonObject body) throws IOException {


        final HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPost.setEntity(new StringEntity(body.toString()));

        logger.info("Successfully posted json {} bytes length", body.toString().length());

        return sendHttpRequest(httpPost, null);
    }

    public static JsonElement getJson(final String url,
                                      final UsernamePasswordCredentials credentials) {

        final HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(HTTP.CONTENT_TYPE, "application/json");

        final String content = sendHttpRequest(httpGet, credentials);

        return new JsonParser().parse(content);
    }


    public static JsonElement putJson(final String url,
                                      final JsonObject body,
                                      final UsernamePasswordCredentials credentials) {

        final HttpPut httpPut = new HttpPut(url);
        httpPut.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPut.setEntity(createStringEntity(body));

        final String content = sendHttpRequest(httpPut, credentials);

        logger.info("Successfully put json {} bytes length", body.toString().length());

        return new JsonParser().parse(content);
    }

    private static StringEntity createStringEntity(final JsonObject body) {
        try {
            return new StringEntity(body.toString());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public static String sendHttpRequest(final HttpUriRequest request,
                                         final UsernamePasswordCredentials credentials) {

        CloseableHttpClient httpClient = HttpClients.createDefault();

        logger.info("Sending {} request to {}", request.getMethod(), request.getURI());
        try {
            auth(request, request.getURI().toURL(), credentials);
            final CloseableHttpResponse response = httpClient.execute(request);
            final StatusLine statusLine = response.getStatusLine();
            final int statusCode = statusLine.getStatusCode();
            if (statusCode >= 400) {
                throw new RuntimeException(String.format("Got %s response", statusCode));
            }

            final HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                throw new RuntimeException("Null response received");
            }

            final String result = EntityUtils.toString(responseEntity);
            EntityUtils.consume(responseEntity);
            logger.info("Successfully consumed response entity");
            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void auth(final HttpRequest request,
                             final URL url,
                             UsernamePasswordCredentials credentials) {

        if (credentials == null) {
            credentials = retrieveCredentialsFromUrl(url);
        }

        try {
            final Header header = new BasicScheme()
                    .authenticate(credentials, request, null);
            request.addHeader(header);
        } catch (AuthenticationException e) {
            throw new RuntimeException(e);
        }

    }

    private static UsernamePasswordCredentials retrieveCredentialsFromUrl(final URL url) {
        final String userInfo = url.getUserInfo();

        if (userInfo == null) {
            throw new IllegalArgumentException("User info is missing in the given url: " + url);
        }


        String decodedUserInfo = urlDecode(userInfo);

        final String[] userAndPassword = decodedUserInfo.split(":");

        if (userAndPassword.length != 2) {
            throw new IllegalArgumentException("Either username or password is missing");
        }

        return new UsernamePasswordCredentials(userAndPassword[0], userAndPassword[1]);
    }

    private static String urlDecode(final String input) {
        try {
            return URLDecoder.decode(input, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
