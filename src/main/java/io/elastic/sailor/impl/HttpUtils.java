package io.elastic.sailor.impl;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;

import org.apache.commons.io.IOUtils;
import org.apache.hc.client5.http.HttpRequestRetryStrategy;
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.elastic.api.JSON;
import io.elastic.sailor.UnexpectedStatusCodeException;
import jakarta.json.JsonObject;

public class HttpUtils {

    private static final Logger logger = LoggerFactory.getLogger(HttpUtils.class.getName());

    private static final ArrayList<CloseableHttpClient> httpClients = new ArrayList<CloseableHttpClient>();

    public static String postJson(
        final String url,
        final CloseableHttpClient httpClient,
        final JsonObject body,
        final AuthorizationHandler authorizationHandler
    ) {
        final HttpPost httpPost = new HttpPost(sanitizeUrl(url));
        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setEntity(createStringEntity(body));

        final byte[] bytes = sendHttpRequest(
                httpPost, httpClient, authorizationHandler, new ByteArrayHttpEntityCallback());

        if (bytes == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully posted json {} bytes length", body.toString().length());

        return new String(bytes);
    }

    public static JsonObject post(
        final String url,
        final CloseableHttpClient httpClient,
        final HttpEntity body,
        final AuthorizationHandler authorizationHandler
    ) {
        final HttpPost httpPost = new HttpPost(sanitizeUrl(url));
        httpPost.setEntity(body);

        final JsonObject result = sendHttpRequest(
                httpPost, httpClient, authorizationHandler, new JsonObjectParseCallback());

        if (result == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully posted content");

        return result;
    }

    public static JsonObject getJson(
        final String url,
        final CloseableHttpClient httpClient,
        final AuthorizationHandler authorizationHandler
    ) {

        final HttpGet httpGet = new HttpGet(sanitizeUrl(url));
        httpGet.addHeader(HttpHeaders.USER_AGENT, "eio-sailor-java");

        final JsonObject content = sendHttpRequest(
                httpGet, httpClient, authorizationHandler, new JsonObjectParseCallback());

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        return content;
    }

    public static byte[] get(
        final String url,
        final CloseableHttpClient httpClient,
        final AuthorizationHandler authorizationHandler
    ) {

        final HttpGet httpGet = new HttpGet(sanitizeUrl(url));
        httpGet.addHeader(HttpHeaders.USER_AGENT, "eio-sailor-java");

        final byte[] content = sendHttpRequest(
                httpGet, httpClient, authorizationHandler, new ByteArrayHttpEntityCallback());

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        return content;
    }


    public static JsonObject putJson(
        final String url,
        final CloseableHttpClient httpClient,
        final JsonObject body,
        final AuthorizationHandler authorizationHandler
    ) {
        final HttpPut httpPut = new HttpPut(sanitizeUrl(url));
        httpPut.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPut.setEntity(createStringEntity(body));

        final JsonObject content = sendHttpRequest(
                httpPut, httpClient, authorizationHandler, new JsonObjectParseCallback());

        if (content == null) {
            throw new RuntimeException("Null response received");
        }

        logger.info("Successfully put json {} bytes length", body.toString().length());

        return content;
    }


    public static void delete(
        final String url,
        final CloseableHttpClient httpClient,
        final AuthorizationHandler authorizationHandler
    ) {
        final HttpDelete httpDelete = new HttpDelete(sanitizeUrl(url));
        httpDelete.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        sendHttpRequest(httpDelete, httpClient, authorizationHandler, null);

        logger.info("Successfully sent delete");

        return;
    }

    public static StringEntity createStringEntity(final JsonObject body) {
        return new StringEntity(JSON.stringify(body));
    }

    public static CloseableHttpClient createHttpClient(final int retryCount) {
        final CloseableHttpClient httpClient = HttpClients.custom()
            .setRetryStrategy(new HttpRequestRetryStrategy() {
                @Override
                public boolean retryRequest(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                    if (execCount >= retryCount) {
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
                }
                @Override
                public boolean retryRequest(HttpResponse response, int execCount, HttpContext context) {
                    final int statusCode = response.getCode();
                    final boolean shouldRetry = statusCode >= 500 && execCount <= retryCount;
                    if (shouldRetry) {
                        logger.warn(response.toString());
                        logger.warn("Error {} during request, retrying ({}/{})", statusCode, execCount, retryCount);
                    }
                    return shouldRetry;
                }
                private static final long MAX_BACKOFF = 15000; // 15 seconds
                private TimeValue getRetryInterval(int execCount) {
                    final double delay = Math.pow(2, execCount) * 1000;
                    final double randomSum = delay * 0.2 * Math.random(); // 0-20% of the delay
                    return TimeValue.ofMilliseconds((long) Math.min(delay + randomSum, MAX_BACKOFF));
                }
                @Override
                public TimeValue getRetryInterval(HttpRequest request, IOException exception, int execCount, HttpContext context) {
                    return getRetryInterval(execCount);
                }
                @Override
                public TimeValue getRetryInterval(HttpResponse response, int execCount, HttpContext context) {
                    return getRetryInterval(execCount);
                }
            })
            .build();
        httpClients.add(httpClient);
        return httpClient;
    }

    public static void closeHttpClients() {
        for (CloseableHttpClient client : httpClients) {
            try {
                client.close();
            } catch (IOException e) {
                logger.warn("Failed to close HTTP client: {}", e.getMessage());
            }
        }
        httpClients.clear();
    }

    private static  <T> T sendHttpRequest(
        final HttpUriRequest request,
        final CloseableHttpClient httpClient,
        final AuthorizationHandler authorizationHandler,
        final HttpEntityCallback<T> callback
    ) {
        try {
            logger.info("Sending {} request to {}", request.getMethod(), request.getUri().getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        authorizationHandler.authorize(request);

        try {
            return httpClient.execute(request, response -> {
                final int statusCode = response.getCode();
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
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    private static UsernamePasswordCredentials retrieveCredentialsFromUri(final URI uri) {
        final String userInfo = uri.getUserInfo();

        if (userInfo == null) {
            throw new IllegalArgumentException("User info is missing in the given url: " + uri);
        }

        String decodedUserInfo = urlDecode(userInfo);

        final String[] userAndPassword = decodedUserInfo.split(":");

        if (userAndPassword.length != 2) {
            throw new IllegalArgumentException("Either username or password is missing");
        }

        return new UsernamePasswordCredentials(userAndPassword[0], userAndPassword[1].toCharArray());
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
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }

        consume(entity);

        return result;
    }

    private static URI sanitizeUrl(final String url) {
        URI originalUri;
        URI sanitizedUri;
		try {
            originalUri = new URI(url);
			sanitizedUri = new URI(
			    originalUri.getScheme(),
			    null, // remove user info
			    originalUri.getHost(),
			    originalUri.getPort(),
			    originalUri.getPath(),
			    originalUri.getQuery(),
			    originalUri.getFragment()
			);
		} catch (URISyntaxException e) {
            throw new RuntimeException("Invalid url " + url);
		}
        return sanitizedUri;
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
            UsernamePasswordCredentials creds = createCredentials(request);
            String auth = creds.getUserName() + ":" + String.valueOf(creds.getUserPassword());
            String encodedAuth = Base64.encodeBase64String(auth.getBytes());
            request.addHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth);
        }
    }

    public static class BasicAuthorizationHandler extends AbstractBasicAuthorizationHandler {
        private UsernamePasswordCredentials credentials;

        public BasicAuthorizationHandler(final String username, final String password) {
            this.credentials = new UsernamePasswordCredentials(username, password.toCharArray());
        }
        public BasicAuthorizationHandler(final String url) {
            try {
                this.credentials = retrieveCredentialsFromUri(new URI(url));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URL provided: " + url, e);
            }
        }

        @Override
        UsernamePasswordCredentials createCredentials(final HttpUriRequest request) {
            return credentials;
        }

        public String getUsername() {
            return credentials.getUserName();
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
