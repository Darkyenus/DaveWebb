package com.darkyen.dave;

import java.io.*;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Entry-point for the HTTP(S) client, with its settings and methods to do work.
 *
 * @author hgoebl
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class Webb {

    private final String baseUri;

    private int connectTimeout = 10000; // 10 seconds
    private int readTimeout = 3 * 60000; // 3 minutes
    private boolean followRedirects = true;

    private Map<String, Object> defaultHeaders = null;
    private SSLSocketFactory sslSocketFactory = null;
    private HostnameVerifier hostnameVerifier = null;
    private RetryManager retryManager = RetryManager.DEFAULT;

    ExecutionStrategy executionStrategy = ExecutionStrategy.SYNCHRONOUS_EXECUTION_STRATEGY;

    /**
     * @param baseUri For all requests this value is taken as a kind of prefix for the effective URI, so you can address
     *                  the URIs relatively. null means no prefix.
     */
    public Webb(String baseUri) {
        this.baseUri = baseUri;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html#setInstanceFollowRedirects(boolean)"></a>
     * @param followRedirects <code>true</code> to automatically follow redirects (HTTP status code 3xx).
     */
    public void setFollowRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
    }

    /**
     * Set the timeout in milliseconds for connecting the server.
     * <br>
     * Default timeout is 10 seconds.
     * <br>
     * Can be overwritten for each Request with {@link Request#connectTimeout(int)}.
     * @param connectTimeout the new timeout or <code>&lt;= 0</code> to disable timeouts.
     */
    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Set the timeout in milliseconds for getting response from the server.
     * <br>
     * Default timeout is 3 minutes.
     * <br>
     * Can be overwritten for each Request with {@link Request#readTimeout(int)}.
     * @param readTimeout the new timeout or <code>&lt;= 0</code> to disable timeouts.
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Set a custom {@link javax.net.ssl.SSLSocketFactory}, most likely to relax Certification checking.
     * @param sslSocketFactory the factory to use (see test cases for an example).
     */
    public void setSSLSocketFactory(SSLSocketFactory sslSocketFactory) {
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * Set a custom {@link javax.net.ssl.HostnameVerifier}, most likely to relax host-name checking.
     * @param hostnameVerifier the verifier (see test cases for an example).
     */
    public void setHostnameVerifier(HostnameVerifier hostnameVerifier) {
        this.hostnameVerifier = hostnameVerifier;
    }

    /**
     * There are three modes of operation with base URI:
     * 1. Base URI is null, all request paths must be absolute, including protocol.
     * 2. Request path starts with "http://" or "https://", that path is taken as absolute, irregardless of what base URI is.
     * 3. Otherwise: Final request path is concatenation of baseUri and given request path.
     *
     * Note that http and https are not the only protocols supported, the whole list depends on the platform.
     * However, only http and https are recognized in mode 2.
     *
     * @return the base URI of this instance.
     */
    public String getBaseUri() {
        return baseUri;
    }

    /**
     * Set the value for a named header which is valid for all requests created by this instance.
     * <br>
     * The value can be overwritten by
     * {@link Request#header(String, Object)}.
     * <br>
     * For the supported types for values see {@link Request#header(String, Object)}.
     *
     * @param name name of the header (regarding HTTP it is not case-sensitive, but here case is important).
     * @param value value of the header. If <code>null</code> the header value is cleared (effectively not set).
     *              When setting the value to null, a value from global headers can shine through.
     *
     * @see Request#header(String, Object)
     */
    public void setDefaultHeader(String name, Object value) {
        if (defaultHeaders == null) {
            defaultHeaders = new HashMap<String, Object>();
        }
        if (value == null) {
            defaultHeaders.remove(name);
        } else {
            defaultHeaders.put(name, value);
        }
    }

    /**
     * Registers an alternative {@link RetryManager}.
     * @param retryManager the new manager for deciding whether it makes sense to retry a request. Not null.
     */
    public void setRetryManager(RetryManager retryManager) {
        if (retryManager == null) throw new NullPointerException("retryManager");
        this.retryManager = retryManager;
    }

    /**
     * Change strategy of asynchronous request execution.
     *
     * Default strategy is {@link ExecutionStrategy#SYNCHRONOUS_EXECUTION_STRATEGY}, which is not async at all.
     *
     * @param executionStrategy not null
     */
    public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
        if (executionStrategy == null) throw new NullPointerException("executionStrategy");
        this.executionStrategy = executionStrategy;
    }

    /**
     * Creates a <b>GET HTTP</b> request with the specified absolute or relative URI.
     * @param pathOrUri the URI - see {@link #getBaseUri()}
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public Request get(String pathOrUri) {
        return new Request(this, HttpMethod.GET, buildPath(pathOrUri));
    }

    /**
     * Creates a <b>POST</b> HTTP request with the specified absolute or relative URI.
     * @param pathOrUri the URI - see {@link #getBaseUri()}
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public Request post(String pathOrUri) {
        return new Request(this, HttpMethod.POST, buildPath(pathOrUri));
    }

    /**
     * Creates a <b>PUT</b> HTTP request with the specified absolute or relative URI.
     * @param pathOrUri the URI - see {@link #getBaseUri()}
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public Request put(String pathOrUri) {
        return new Request(this, HttpMethod.PUT, buildPath(pathOrUri));
    }

    /**
     * Creates a <b>DELETE</b> HTTP request with the specified absolute or relative URI.
     * @param pathOrUri the URI - see {@link #getBaseUri()}
     * @return the created Request object (in fact it's more a builder than a real request object)
     */
    public Request delete(String pathOrUri) {
        return new Request(this, HttpMethod.DELETE, buildPath(pathOrUri));
    }

    private String buildPath(String pathOrUri) {
        if (pathOrUri == null) {
            throw new IllegalArgumentException("pathOrUri must not be null");
        }
        if (baseUri != null) {
            if (pathOrUri.startsWith("http://") || pathOrUri.startsWith("https://")) {
                return pathOrUri;
            }
            return baseUri + pathOrUri;
        } else {
            return pathOrUri;
        }
    }

    <T> Response<T> execute(Request request, ResponseTranslator<T> translator) {
        Response<T> response = null;

        if (request.retryCount == 0) {
            // no retry -> just delegate to inner method
            response = _execute(request, translator);
        } else {
            for (int tries = 0; tries <= request.retryCount; ++tries) {
                try {
                    response = _execute(request, translator);
                    if (tries >= request.retryCount || !retryManager.isRetryUseful(response)) {
                        break;
                    }
                } catch (WebbException we) {
                    // analyze: is exception recoverable?
                    if (tries >= request.retryCount || !retryManager.isRecoverable(we)) {
                        throw we;
                    }
                }
                if (request.waitExponential) {
                    retryManager.wait(tries);
                }
            }
        }
        if (response == null) {
            throw new IllegalStateException(); // should never reach this line
        }
        if (request.ensureSuccess) {
            response.ensureSuccess();
        }

        return response;
    }

    private <T> Response<T> _execute(Request request, ResponseTranslator<T> translator) {
        InputStream is = null;
        final URLConnection connection;
        HttpURLConnection httpConnection = null;

        Response<T> response = null;

        try {
            String uri = request.uri;
            if (!request.method.canHaveBody && request.params != null && !request.params.isEmpty()) {
                if (uri.indexOf('?') != -1) {
                    uri = uri + '&' + WebbUtils.queryString(request.params);
                } else {
                    uri = uri + '?' + WebbUtils.queryString(request.params);
                }
            }
            URL apiUrl = new URL(uri);
            connection = apiUrl.openConnection();
            if (connection instanceof HttpURLConnection) {
                httpConnection = (HttpURLConnection) connection;
            }

            prepareSslConnection(connection);
            if (httpConnection != null) {
                httpConnection.setRequestMethod(request.method.name());
                httpConnection.setInstanceFollowRedirects(request.followRedirects == null ? followRedirects : request.followRedirects);
            }
            connection.setUseCaches(request.useCaches);
            connection.setConnectTimeout(request.connectTimeout == null ? connectTimeout : request.connectTimeout);
            connection.setReadTimeout(request.readTimeout == null ? readTimeout : request.readTimeout);
            if (request.ifModifiedSince != null) {
                connection.setIfModifiedSince(request.ifModifiedSince);
            }

            WebbUtils.addRequestProperties(connection, mergeHeaders(request.headers));

            if (request.method.canHaveBody) {
                final BodyStreamProvider payloadStream = request.payloadStream;
                final byte[] payloadData = request.payloadData;

                if (payloadStream != null) {
                    WebbUtils.ensureRequestProperty(connection, WebbConst.HDR_CONTENT_TYPE, request.payloadContentType != null ? request.payloadContentType : WebbConst.MIME_BINARY);

                    InputStream stream = payloadStream.createStream();

                    if (httpConnection != null) {
                        if (request.compressPayload) {
                            httpConnection.setChunkedStreamingMode(-1); // use default chunk size
                            httpConnection.setRequestProperty(WebbConst.HDR_CONTENT_ENCODING, "gzip");
                        } else {
                            //noinspection unchecked
                            final long length = payloadStream.payloadSize(stream);
                            boolean lengthSet = false;

                            // - 8 just in case, values too close to close to MAX_VALUE may be buggy
                            if (length > 0 && length <= Integer.MAX_VALUE - 8) {
                                // This is fine, we can set this always
                                httpConnection.setFixedLengthStreamingMode((int)length);
                                lengthSet = true;
                            } else if (length > 0) {
                                // This should be fine on newer implementations...
                                try {
                                    //noinspection Since15
                                    httpConnection.setFixedLengthStreamingMode(length);
                                    lengthSet = true;
                                } catch (NoSuchMethodError ignored) {
                                    // ... and this is not a new implementation
                                }
                            }

                            if (!lengthSet) {
                                // Length can't be set normally, send it in parts
                                httpConnection.setChunkedStreamingMode(-1); // use default chunk size
                            }
                        }
                    }


                    connection.setDoOutput(true);

                    // "E/StrictMode﹕ A resource was acquired at attached stack trace but never released"
                    // see comments about this problem in #writeBody()
                    OutputStream os = null;
                    try {
                        os = connection.getOutputStream();

                        if (request.compressPayload) {
                            GZIPOutputStream gos = new GZIPOutputStream(os);
                            WebbUtils.copyStream(stream, gos);
                            gos.finish();
                        } else {
                            WebbUtils.copyStream(stream, os);
                        }

                        os.flush();
                    } finally {
                        if (os != null) {
                            try { os.close(); } catch (Exception ignored) {}
                        }

                        //noinspection unchecked
                        payloadStream.destroyStream(stream);
                    }
                } else if (payloadData != null || request.params != null) {
                    byte[] sentPayloadData;
                    if (payloadData == null) {
                        sentPayloadData = WebbUtils.queryString(request.params).getBytes(WebbConst.UTF8);
                        WebbUtils.ensureRequestProperty(connection, WebbConst.HDR_CONTENT_TYPE, WebbConst.MIME_URLENCODED);
                    } else {
                        sentPayloadData = payloadData;
                        WebbUtils.ensureRequestProperty(connection, WebbConst.HDR_CONTENT_TYPE, request.payloadContentType != null ? request.payloadContentType : WebbConst.MIME_BINARY);
                    }


                    // only compress if the new body is smaller than uncompressed body
                    if (request.compressPayload && sentPayloadData.length > WebbConst.MIN_COMPRESSED_ADVANTAGE) {
                        byte[] compressedBody = WebbUtils.gzip(payloadData);
                        if (sentPayloadData.length - compressedBody.length > WebbConst.MIN_COMPRESSED_ADVANTAGE) {
                            sentPayloadData = compressedBody;
                            connection.setRequestProperty(WebbConst.HDR_CONTENT_ENCODING, "gzip");
                        }
                    }

                    // We don't need to setFixedLengthStreamingMode, because we already know that given data will fit
                    // into internal buffers - we already have it in memory!
                    connection.setDoOutput(true);
                    writeBody(connection, sentPayloadData);
                }
            }
            connection.connect();

            response = new Response<T>(request, connection);

            // get the response body (if any)
            if (response.isSuccess()) {
                is = connection.getInputStream();
            } else if (httpConnection != null) {
                is = httpConnection.getErrorStream();
                if (is == null) {
                    is = connection.getInputStream();
                }
            }
            is = WebbUtils.decodeStream(connection.getContentEncoding(), is);

            if (translator == null) {
                response.body = null;
            } else if (is != null) {
                response.body = translator.decode(response, is);
            } else {
                response.body = translator.decodeEmptyBody(response);
            }

            return response;
        } catch (WebbException e) {
            e.response = response;
            throw e;
        } catch (Exception e) {
            final WebbException exception = new WebbException(e);
            exception.response = response;
            throw exception;
        } finally {
            WebbUtils.closeQuietly(is);
            if (httpConnection != null) {
                try { httpConnection.disconnect(); } catch (Exception ignored) {}
            }
        }
    }

    private void writeBody(URLConnection connection, byte[] body) throws IOException {
        // Android StrictMode might complain about not closing the connection:
        // "E/StrictMode﹕ A resource was acquired at attached stack trace but never released"
        // It seems like some kind of bug in special devices (e.g. 4.0.4/Sony) but does not
        // happen e.g. on 4.4.2/Moto G.
        // Closing the stream in the try block might help sometimes (it's intermittently),
        // but I don't want to deal with the IOException which can be thrown in close().
        OutputStream os = null;
        try {
            os = connection.getOutputStream();
            os.write(body);
            os.flush();
        } finally {
            if (os != null) {
                try { os.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void prepareSslConnection(URLConnection connection) {
        if (connection instanceof HttpsURLConnection) {
            HttpsURLConnection sslConnection = (HttpsURLConnection) connection;
            if (hostnameVerifier != null) {
                sslConnection.setHostnameVerifier(hostnameVerifier);
            }
            if (sslSocketFactory != null) {
                sslConnection.setSSLSocketFactory(sslSocketFactory);
            }
        }
    }

    private Map<String, Object> mergeHeaders(Map<String, Object> requestHeaders) {
        Map<String, Object> headers = null;
        if (defaultHeaders != null) {
            headers = new LinkedHashMap<String, Object>();
            headers.putAll(defaultHeaders);
        }
        if (requestHeaders != null) {
            if (headers == null) {
                headers = requestHeaders;
            } else {
                headers.putAll(requestHeaders);
            }
        }
        return headers;
    }
}
