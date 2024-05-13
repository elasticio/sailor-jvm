package io.elastic.sailor.impl;

import io.elastic.api.JSON;
import io.elastic.sailor.UnexpectedStatusCodeException;
import org.apache.commons.io.IOUtils;
import org.apache.http.*;

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

import jakarta.json.JsonObject;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.URLDecoder;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class.getName());

    public static String postJson(String url, JsonObject body, AuthorizationHandler authorizationHandler) throws IOException {
        return postJson(url, body, authorizationHandler, 0);
    }

    public static JsonObject getJson(String url, final AuthorizationHandler authorizationHandler) {
        return getJson(url, authorizationHandler, 0);
    }

    public static JsonObject putJson(final String url,
                                     final JsonObject body,
                                     final AuthorizationHandler authorizationHandler) {
        return putJson(url, body, authorizationHandler, 0);
    }

    public static String postJson(final String url,
                                  final JsonObject body,
                                  final AuthorizationHandler authorizationHandler,
                                  final int retryCount) {


        final HttpPost httpPost = new HttpPost(url);
        httpPost.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPost.setEntity(createStringEntity(body));

        final byte[] bytes = sendHttpRequest(
                httpPost, authorizationHandler, retryCount, new ByteArrayHttpEntityCallback());

        if (bytes == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully posted json {} bytes length", body.toString().length());

        return new String(bytes);
    }

    public static JsonObject post(final String url,
                              final HttpEntity body,
                              final AuthorizationHandler authorizationHandler,
                              final int retryCount) {


        final HttpPost httpPost = new HttpPost(url);
        httpPost.setEntity(body);

        final JsonObject result = sendHttpRequest(
                httpPost, authorizationHandler, retryCount, new JsonObjectParseCallback());

        if (result == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully posted content");

        return result;
    }

    public static JsonObject getJson(final String url,
                                     final AuthorizationHandler authorizationHandler,
                                     int retryCount) {

        final HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(HTTP.USER_AGENT, "eio-sailor-java");

        final JsonObject content = sendHttpRequest(
                httpGet, authorizationHandler, retryCount, new JsonObjectParseCallback());

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        return content;
    }

    public static byte[] get(final String url,
                                 final AuthorizationHandler authorizationHandler,
                                 int retryCount) {

        final HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader(HTTP.USER_AGENT, "eio-sailor-java");

        final byte[] content = sendHttpRequest(
                httpGet, authorizationHandler, retryCount, new ByteArrayHttpEntityCallback());

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        return content;
    }


    public static JsonObject putJson(final String url,
                                     final JsonObject body,
                                     final AuthorizationHandler authorizationHandler,
                                     final int retryCount) {

        final HttpPut httpPut = new HttpPut(url);
        httpPut.addHeader(HTTP.CONTENT_TYPE, "application/json");
        httpPut.setEntity(createStringEntity(body));

        final JsonObject content = sendHttpRequest(
                httpPut, authorizationHandler, retryCount, new JsonObjectParseCallback());

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully put json {} bytes length", body.toString().length());

        return content;
    }


    public static void delete(final String url,
                              final AuthorizationHandler authorizationHandler,
                              final int retryCount) {

        final HttpDelete httpDelete = new HttpDelete(url);
        httpDelete.addHeader(HTTP.CONTENT_TYPE, "application/json");

        sendHttpRequest(httpDelete, authorizationHandler, retryCount, null);

        logger.info("Successfully sent delete");

        return;
    }

    public static StringEntity createStringEntity(final JsonObject body) {
        return createStringEntity(JSON.stringify(body));
    }

    public static StringEntity createStringEntity(final String content) {
        try {
            return new StringEntity(content);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static  <T> T sendHttpRequest(final HttpUriRequest request,
                                        final AuthorizationHandler authorizationHandler,
                                        final int retryCount,
                                        final HttpEntityCallback<T> callback) {

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

        logger.info("Sending {} request to {}", request.getMethod(), request.getURI().getPath());
        try {
            authorizationHandler.authorize(request);
            logger.info("Got 1");
            final CloseableHttpResponse response = httpClient.execute(request);
            logger.info("Got 2", response);
            final StatusLine statusLine = response.getStatusLine();
            logger.info("Got 3", statusLine);
            final int statusCode = statusLine.getStatusCode();
            logger.info("Got {} response", statusCode);
            if (statusCode >= 400) {
                throw new UnexpectedStatusCodeException(statusCode);
            }

            final HttpEntity responseEntity = response.getEntity();

            if (responseEntity == null || callback == null) {
                return null;
            }

            final T result = callback.handle(responseEntity);

            EntityUtils.consume(responseEntity);

            return result;

        } catch (Exception e) {
            logger.error("Failed to send request: {}", e);
            throw new RuntimeException(e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private interface HttpEntityCallback<T> {
        T handle(HttpEntity entity);
    }

    private static class JsonObjectParseCallback implements HttpEntityCallback<JsonObject> {

        @Override
        public JsonObject handle(HttpEntity entity) {
            if (entity == null) {
                throw new RuntimeException("Null response received");
            }

            return JSON.parseObject(consumeToString(entity));
        }
    }

    private static class ByteArrayHttpEntityCallback implements HttpEntityCallback<byte[]> {
        @Override
        public byte[] handle(HttpEntity entity) {
            try {
                return IOUtils.toByteArray(entity.getContent());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
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


    private static String consumeToString(final HttpEntity entity) {
        String result;
        try {
            result = EntityUtils.toString(entity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        consume(entity);

        return result;
    }

    public static void consume(final HttpEntity entity) {
        try {
            EntityUtils.consume(entity);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface AuthorizationHandler {

        void authorize(final HttpUriRequest request);
    }

    static abstract class AbstractBasicAuthorizationHandler implements AuthorizationHandler {

        abstract UsernamePasswordCredentials createCredentials(HttpUriRequest request);

        @Override
        public void authorize(HttpUriRequest request) {
            logger.info("setting up basic auth");

            try {
                final Header header = new BasicScheme()
                        .authenticate(createCredentials(request), request, null);
                logger.info("Adding header {}", header);
                request.addHeader(header);
            } catch (AuthenticationException e) {
                logger.error("Failed to authenticate request: {}", e);
                throw new RuntimeException(e);
            }
        }
    }

    public static class BasicAuthorizationHandler extends AbstractBasicAuthorizationHandler {
        private UsernamePasswordCredentials credentials;

        public BasicAuthorizationHandler(final String username, final String password) {
            this.credentials = new UsernamePasswordCredentials(username, password);
        }

        @Override
        UsernamePasswordCredentials createCredentials(final HttpUriRequest request) {
            return credentials;
        }

        public String getUsername() {
            return credentials.getUserName();
        }
    }

    public static class BasicURLAuthorizationHandler extends AbstractBasicAuthorizationHandler {

        @Override
        UsernamePasswordCredentials createCredentials(final HttpUriRequest request) {
            final URL url = getRequestURL(request);
            return retrieveCredentialsFromUrl(url);
        }

        private URL getRequestURL(final HttpUriRequest request) {
            try {
                return request.getURI().toURL();
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static class BearerAuthorizationHandler implements AuthorizationHandler {
        private String token;

        public BearerAuthorizationHandler(final String token) {
            this.token = token;
        }

        @Override
        public void authorize(HttpUriRequest request) {
            request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
    }
}
