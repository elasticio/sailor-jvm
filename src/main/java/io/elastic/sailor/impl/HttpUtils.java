package io.elastic.sailor.impl;

import io.elastic.api.JSON;
import io.elastic.sailor.UnexpectedStatusCodeException;
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

import javax.json.JsonObject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class.getName());

    public static String postJson(String url, JsonObject body) throws IOException {
        return postJson(url, body, 0);
    }

    public static JsonObject getJson(String url, final UsernamePasswordCredentials credentials) {
        return getJson(url, credentials, 0);
    }

    public static JsonObject putJson(final String url,
                                     final JsonObject body,
                                     final UsernamePasswordCredentials credentials) {
        return putJson(url, body, credentials, 0);
    }

    public static String postJson(String url, JsonObject body, int retryCount) throws IOException {

        return postJson(url, body, null, retryCount);
    }

    public static String postJson(final String url,
                                  final JsonObject body,
                                  final UsernamePasswordCredentials credentials,
                                  final int retryCount) {


        final HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPost.setEntity(createStringEntity(body));

        final String result = sendHttpRequest(httpPost, credentials, retryCount);

        if (result == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully posted json {} bytes length", body.toString().length());

        return result;
    }

    public static JsonObject getJson(final String url,
                                     final UsernamePasswordCredentials credentials, int retryCount) {

        final HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpGet.addHeader(HTTP.USER_AGENT, "eio-sailor-java");

        final String content = sendHttpRequest(httpGet, credentials, retryCount);

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        return JSON.parseObject(content);
    }


    public static JsonObject putJson(final String url,
                                     final JsonObject body,
                                     final UsernamePasswordCredentials credentials,
                                     final int retryCount) {

        final HttpPut httpPut = new HttpPut(url);
        httpPut.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPut.setEntity(createStringEntity(body));

        final String content = sendHttpRequest(httpPut, credentials, retryCount);

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully put json {} bytes length", body.toString().length());

        return JSON.parseObject(content);
    }



    public static void delete(final String url,
                                    final UsernamePasswordCredentials credentials,
                                    final int retryCount) {

        final HttpDelete httpDelete = new HttpDelete(url);
        httpDelete.addHeader(HTTP.CONTENT_TYPE, "application/json");

        sendHttpRequest(httpDelete, credentials, retryCount);

        logger.info("Successfully sent delete");

        return;
    }

    private static StringEntity createStringEntity(final JsonObject body) {
        try {
            return new StringEntity(JSON.stringify(body));
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static String sendHttpRequest(final HttpUriRequest request,
                                          final UsernamePasswordCredentials credentials,
                                          final int retryCount) {

        CloseableHttpClient httpClient = HttpClients.custom()
                .setRetryHandler((exception, executionCount, context) -> {
                    if (executionCount >= retryCount) {
                        // Do not retry if over max retry count
                        return false;
                    }
                    if (exception instanceof InterruptedIOException) {
                        // Timeout
                        return true;
                    }
                    if (exception instanceof SocketException) {
                        return true;
                    }

                    return false;
                })
                .build();

        logger.info("Sending {} request to {}", request.getMethod(), request.getURI());
        try {
            auth(request, request.getURI().toURL(), credentials);
            final CloseableHttpResponse response = httpClient.execute(request);
            final StatusLine statusLine = response.getStatusLine();
            final int statusCode = statusLine.getStatusCode();
            logger.info("Got {} response", statusCode);
            if (statusCode >= 400) {
                throw new UnexpectedStatusCodeException(statusCode);
            }

            final HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null) {
                return null;
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
