package com.goebl.david;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builder for an HTTP request.
 * <br>
 * You can some "real-life" usage examples at
 * <a href="https://github.com/hgoebl/DavidWebb">github.com/hgoebl/DavidWebb</a>.
 * <br>
 *
 * Most methods return this Request for chaining.
 *
 * @author hgoebl
 */
@SuppressWarnings("WeakerAccess")
public final class Request {

    private final Webb webb;

    final HttpMethod method;
    final String uri;

    Map<String, Object> headers;

    Map<String, Object> params;
    boolean multipleValues;

    boolean useCaches = false;
    Integer connectTimeout = null;
    Integer readTimeout = null;
    Long ifModifiedSince = null;
    Boolean followRedirects = null;

    /** Content type of payload or null to guess (or that there is no payload) */
    String payloadContentType = null;
    /** Stream with data to be sent. Full stream will be sent. If not null, payloadData MUST be null. */
    BodyStreamProvider payloadStream;
    /** Bytes to be sent. All bytes will be sent. If not null, payloadStream MUST be null. */
    byte[] payloadData;
    /** Payload will be compressed if this flag is true and the payload is not too small */
    boolean compressPayload;

    boolean ensureSuccess;
    int retryCount;
    boolean waitExponential;

    Request(Webb webb, HttpMethod method, String uri) {
        this.webb = webb;
        this.method = method;
        this.uri = uri;
    }

    /**
     * Turn on a mode where one parameter key can have multiple values.
     * <br>
     * Example: <code>order.php?fruit=orange&amp;fruit=apple&amp;fruit=banana</code>
     * <br>
     * This is only necessary when you want to call {@link #param(String, Object)} multiple
     * times with the same parameter name and this should lead to having multiple values.
     * If you call {@link #param(String, Iterable)} or already provide an Array as value parameter,
     * you don't have to call this method and it should work as expected.
     *
     * @return <code>this</code> for method chaining (fluent API)
     * @since 1.3.0
     */
    public Request multipleValues() {
        multipleValues = true;
        return this;
    }

    /**
     * Set (or overwrite) a parameter.
     * <br>
     * The parameter will be used to create a query string for GET-requests and as the body for POST-requests
     * with MIME-type <code>application/x-www-form-urlencoded</code>.
     * <br>
     * Please see {@link #multipleValues()} if you have to deal with parameters carrying multiple values.
     * <br>
     * Handling of multi-valued parameters exists since version 1.3.0
     *
     * @param name the name of the parameter (it's better to use only contain ASCII characters)
     * @param value the value of the parameter; <code>null</code> will be converted to empty string,
     *              Arrays of Objects are expanded to multiple valued parameters, for all other
     *              objects to <code>toString()</code> method converts it to String
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request param(String name, Object value) {
        if (params == null) {
            params = new LinkedHashMap<String, Object>();
        }
        if (multipleValues) {
            Object currentValue = params.get(name);
            if (currentValue != null) {
                if (currentValue instanceof Collection) {
                    //noinspection unchecked
                    Collection<Object> values = (Collection) currentValue;
                    values.add(value);
                } else {
                    // upgrade single value to set of values
                    Collection<Object> values = new ArrayList<Object>();
                    values.add(currentValue);
                    values.add(value);
                    params.put(name, values);
                }
                return this;
            }
        }
        params.put(name, value);
        return this;
    }

    /**
     * Set (or overwrite) a parameter with multiple values.
     * <br>
     * The parameter will be used to create a query string for GET-requests and as the body for POST-requests
     * with MIME-type <code>application/x-www-form-urlencoded</code>.
     * <br>
     * If you use this method, you don't have to call {@link #multipleValues()}, but you should not mix
     * using {@link #param(String, Object)} and this method for the same parameter name as this might cause
     * unexpected behaviour or exceptions.
     *
     * @param name the name of the parameter (it's better to use only contain ASCII characters)
     * @param values the values of the parameter; will be expanded to multiple valued parameters.
     * @return <code>this</code> for method chaining (fluent API)
     * @since 1.3.0
     */
    public Request param(String name, Iterable<Object> values) {
        if (params == null) {
            params = new LinkedHashMap<String, Object>();
        }
        params.put(name, values);
        return this;
    }

    /**
     * Set (or overwrite) many parameters via a map.
     * <br>
     * @param valueByName a Map of name-value pairs,<br>
     *  the name of the parameter (it's better to use only contain ASCII characters)<br>
     *  the value of the parameter; <code>null</code> will be converted to empty string, for all other
     *              objects to <code>toString()</code> method converts it to String
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request params(Map<String, Object> valueByName) {
        if (params == null) {
            params = new LinkedHashMap<String, Object>();
        }
        params.putAll(valueByName);
        return this;
    }

    /**
     * Get the URI of this request.
     *
     * @return URI
     */
    public String getUri() {
        return uri;
    }

    /**
     * Set (or overwrite) a HTTP header value.
     * <br>
     * Setting a header this way has the highest precedence and overrides a header value set on a {@link Webb}
     * instance ({@link Webb#setDefaultHeader(String, Object)}).
     * <br>
     * Using <code>null</code> or empty String is not allowed for name and value.
     *
     * @param name name of the header (HTTP-headers are not case-sensitive, but if you want to override your own
     *             headers, you have to use identical strings for the name. There are some frequently used header
     *             names as constants in {@link Webb}, see HDR_xxx.
     * @param value the value for the header. Following types are supported, all other types use <code>toString</code>
     *              of the given object:
     *              <ul>
     *              <li>{@link java.util.Date} is converted to RFC1123 compliant String</li>
     *              <li>{@link java.util.Calendar} is converted to RFC1123 compliant String</li>
     *              </ul>
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request header(String name, Object value) {
        if (headers == null) {
            headers = new LinkedHashMap<String, Object>();
        }
        headers.put(name, value);
        return this;
    }

    private void ensureBodyCanBeSet() {
        if (payloadData != null) {
            throw new IllegalStateException("Body is already set to byte[]");
        }
        if (payloadStream != null) {
            throw new IllegalStateException("Body is already set to stream");
        }
        if (params != null) {
            throw new IllegalStateException("Params are already set");
        }
        if (!method.canHaveBody) {
            throw new IllegalStateException("Method "+method+" can't have body");
        }
    }

    /** Convenience method, calls {@link #body(File, String)} with type null. */
    public Request body(final File file) {
        return body(file, null);
    }

    /** Set the payload for this request to the content of given file. Call only once. File will be streamed. */
    public Request body(final File file, final String contentType) {
        ensureBodyCanBeSet();
        if (file == null) throw new NullPointerException("file");

        this.payloadContentType = contentType;
        this.payloadStream = new BodyStreamProvider<FileInputStream>() {
            public FileInputStream createStream() throws Exception {
                return new FileInputStream(file);
            }

            public long payloadSize(FileInputStream forStream) {
                return file.length();
            }

            public void destroyStream(FileInputStream usedStream) {
                WebbUtils.closeQuietly(usedStream);
            }
        };
        return this;
    }

    /** Set the payload for this request to the content of given stream. Call only once. Data will be streamed.
     *
     * Streaming is better and needed when the payload is too big and won't comfortably fit in memory.
     * However, redirection is not supported in streamed mode. Trying to set streamed body and redirection
     * in the same request will throw an IllegalStateException. */
    public Request body(BodyStreamProvider stream, String contentType) {
        ensureBodyCanBeSet();
        if (stream == null) throw new NullPointerException("stream");

        if (this.followRedirects == Boolean.TRUE) {
            throw new IllegalStateException("Can't follow redirects in streamed mode!");
        }
        this.followRedirects = Boolean.FALSE;

        this.payloadContentType = contentType;
        this.payloadStream = stream;
        return this;
    }

    /** Set the payload for this request to the given bytes. Call only once. */
    public Request body(byte[] data, String contentType) {
        ensureBodyCanBeSet();
        if (data == null) throw new NullPointerException("data");

        this.payloadContentType = contentType;
        this.payloadData = data;
        return this;
    }

    /** Convenience method, calls {@link #body(String, String)} with type null. */
    public Request body(String data) {
        return this.body(data, null);
    }

    /** Set the payload for this request to the given UTF8 bytes. Call only once.
     * Content type is set to text/plain if null. */
    public Request body(String data, String contentType) {
        ensureBodyCanBeSet();
        if (data == null) throw new NullPointerException("data");

        try {
            this.payloadData = data.getBytes(WebbConst.UTF8);
            this.payloadContentType = contentType == null ? WebbConst.MIME_TEXT_PLAIN : contentType;
        } catch (UnsupportedEncodingException e) {
            throw new WebbException("Can't sent UTF-8 string", e);
        }
        return this;
    }

    /** Convenience call to {@link #body(String, String)} with json content type */
    public Request bodyJson(String data) {
        return body(data, WebbConst.MIME_JSON);
    }

    /**
     * Enable compression for uploaded data.<br>
     * <br>
     * Before you enable compression, you should find out, whether the web server you are talking to
     * supports this. As compression has not to be implemented for HTTP and standard RFC2616 had only
     * compression for downloaded resources in mind, in special cases it makes absolutely sense to
     * compress the posted data.<br>
     * Your web application should inspect the 'Content-Encoding' header and implement the compression
     * token provided by this client. By now only 'gzip' encoding token is used.
     *
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request compress() {
        compressPayload = true;
        return this;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#useCaches">
     *     URLConnection.useCaches</a>
     * <br>
     * If you don't want your requests delivered from a cache, you don't have to call this method,
     * because <code>false</code> is the default.
     *
     * @param useCaches If <code>true</code>, the protocol is allowed to use caching whenever it can.
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request useCaches(boolean useCaches) {
        this.useCaches = useCaches;
        return this;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#setIfModifiedSince(long)">
     *     URLConnection.setIfModifiedSince()</a>
     * @param ifModifiedSince A nonzero value gives a time as the number of milliseconds since January 1, 1970, GMT.
     *                        The object is fetched only if it has been modified more recently than that time.
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request ifModifiedSince(long ifModifiedSince) {
        this.ifModifiedSince = ifModifiedSince;
        return this;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#setConnectTimeout(int)">
     *     URLConnection.setConnectTimeout</a>
     * @param connectTimeout sets a specified timeout value, in milliseconds. <code>0</code> means infinite timeout.
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request connectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#setReadTimeout(int)">
     *     </a>
     * @param readTimeout Sets the read timeout to a specified timeout, in milliseconds.
     *                    <code>0</code> means infinite timeout.
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html#setInstanceFollowRedirects(boolean)">
     *     </a>.
     * <br>
     * Use this method to set the behaviour for this single request when receiving redirect responses.
     * If you want to change the behaviour for all your requests, use {@link Webb#setFollowRedirects(boolean)}.
     *
     * @see #body(BodyStreamProvider, String) for caveat about streaming
     * @param follow <code>true</code> to automatically follow redirects (HTTP status code 3xx).
     *             Default value comes from HttpURLConnection and should be <code>true</code>.
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request followRedirects(boolean follow) {
        if (follow && payloadStream != null) {
            throw new IllegalStateException("Can't enable following redirects when payload is streamed!");
        }
        this.followRedirects = follow;
        return this;
    }

    /**
     * By calling this method, the HTTP status code is checked and a <code>WebbException</code> is thrown if
     * the status code is not something like 2xx.<br>
     * <br>
     * Be careful! If you request resources e.g. with {@link #ifModifiedSince(long)}, an exception will also be
     * thrown in the positive case of <code>304 Not Modified</code>.
     *
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request ensureSuccess() {
        this.ensureSuccess = true;
        return this;
    }

    /**
     * Set the number of retries after the first request failed.
     * <br>
     * When `waitExponential` is set, then there will be {@link Thread#sleep(long)} between
     * the retries. If the thread is interrupted, there will be an `InterruptedException`
     * in the thrown `WebbException`. You can check this with {@link WebbException#getCause()}.
     * The `interrupted` flag will be set to true in this case.
     *
     * @param retryCount This parameter holds the number of retries that will be made AFTER the
     *                   initial send in the event of a error. If an error occurs on the last
     *                   attempt an exception will be raised.<br>
     *                   Values &gt; 10 are ignored (we're not gatling)
     * @param waitExponential sleep during retry attempts (exponential backoff).
     *                        For retry-counts more than 3, <tt>true</tt> is mandatory.
     * @return <code>this</code> for method chaining (fluent API)
     */
    public Request retry(int retryCount, boolean waitExponential) {
        if (retryCount < 0) {
            retryCount = 0;
        }
        if (retryCount > 10) {
            retryCount = 10;
        }
        if (retryCount > 3 && !waitExponential) {
            throw new IllegalArgumentException("retries > 3 only valid with wait");
        }
        this.retryCount = retryCount;
        this.waitExponential = waitExponential;
        return this;
    }

    /**
     * Execute the request with given translator.
     * @return the created <code>Response</code> object carrying the payload from the server as <code>T</code>
     */
    public <T> Response<T> execute(ResponseTranslator<T> translator) {
        return webb.execute(this, translator);
    }

    /**
     * Execute the request with default string translator.
     * @return the created <code>Response</code> object carrying the payload from the server as <code>String</code>
     */
    public Response<String> executeString() {
        return webb.execute(this, ResponseTranslator.STRING_TRANSLATOR);
    }

    /**
     * Execute the request with default byte[] translator.
     * @return the created <code>Response</code> object carrying the payload from the server as <code>byte[]</code>
     */
    public Response<byte[]> executeBytes() {
        return webb.execute(this, ResponseTranslator.BYTES_TRANSLATOR);
    }

    /**
     * Execute the request and expect no result payload (only status-code and headers).
     * @return the created <code>Response</code> object where no payload is expected or simply will be ignored.
     */
    public Response<Void> execute() {
        return webb.execute(this, null);
    }
}
