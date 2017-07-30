package com.goebl.david;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.goebl.david.WebbUtils.RFC1123_DATE_FORMAT;

/**
 * Holds data about the response message returning from HTTP request.
 *
 * @author hgoebl
 */
@SuppressWarnings("WeakerAccess")
public final class Response<T> {

    private final Request request;

    private final int statusCode;
    private final String statusMessage;
    private final String statusLine;

    private final Map<String, List<String>> headers;
    private final String contentType;
    private final long date, expiration, lastModified;

    T body;

    Response(Request request, URLConnection connection) throws IOException {
        this.request = request;

        if (connection instanceof HttpURLConnection) {
            this.statusCode = ((HttpURLConnection)connection).getResponseCode();
            this.statusMessage = ((HttpURLConnection)connection).getResponseMessage();
        } else {
            this.statusCode = 200;
            this.statusMessage = "Non-http connection";
        }
        this.statusLine = connection.getHeaderField(null);

        this.headers = connection.getHeaderFields();
        this.contentType = connection.getContentType();
        this.date = connection.getDate();
        this.expiration = connection.getExpiration();
        this.lastModified = connection.getLastModified();
    }

    /**
     * Access to the <code>Request</code> object (which will not be very useful in most cases).
     * @return the request object which was responsible for creating this response.
     */
    public Request getRequest() {
        return request;
    }

    /**
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/HttpURLConnection.html#responseCode">
     *     HttpURLConnection.responseCode</a>
     * @return An int representing the three digit HTTP Status-Code.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns the text explaining the status code.
     * @return e.g. "Moved Permanently", "Created", ...
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    /**
     * The first line returned by the web-server, like "HTTP/1.1 200 OK".
     * @return first header
     */
    public String getStatusLine() {
        return statusLine;
    }

    /**
     * Was the request successful (returning a 2xx status code)?
     * @return <code>true</code> when status code is between 200 and 299, else <code>false</code>
     */
    public boolean isSuccess() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Returns the MIME-type of the response body.
     * <br>
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#getContentType()">
     *     URLConnection.getContentType()</a>
     *
     * @return e.g. "application/json", "text/plain", ...
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Returns the payload of the response converted to the given type.
     * @return the converted payload (can be null).
     */
    public T getBody() {
        return body;
    }

    /**
     * Returns the date when the request was created (server-time).
     * <br>
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#getDate()">
     *     URLConnection.getDate()</a>
     *
     * @return the parsed "Date" header as millis or <code>0</code> if this header was not set.
     */
    public long getDate() {
        return date;
    }

    /**
     * Returns the value of the expires header field.
     * <br>
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#getExpiration()">
     *     URLConnection.getExpiration()</a>
     *
     * @return the expiration date of the resource, or 0 if not known.
     */
    public long getExpiration() {
        return expiration;
    }

    /**
     * Returns the value of the last-modified header field.
     * <br>
     * See <a href="http://docs.oracle.com/javase/7/docs/api/java/net/URLConnection.html#getLastModified()">
     *     URLConnection.getLastModified()</a>
     *
     * @return the date the resource was last modified, or 0 if not known.
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Returns the value of the named header field.
     * If there is multiple values, last is returned.
     *
     * @param name of the header field
     * @return the value of the named header field, or null
     */
    public String getHeaderField (String name) {
        final List<String> values = headers.get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(values.size() - 1);
    }

    /**
     * Returns the value of the named field parsed as a number.
     *
     * @param name of the header field
     * @param defaultValue the default value if the field is not present or malformed
     * @return the value of the named header field, or the given default value
     * @see #getHeaderField(String) for header field resolution
     */
    public long getHeaderFieldInt(String name, long defaultValue) {
        final String value = getHeaderField(name);
        if (value == null) return defaultValue;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Returns the value of the named field parsed as a date (ms since epoch).
     *
     * @param name of the header field
     * @param defaultValue the default value if the field is not present or malformed
     * @return the value of the named header field, or the given default value
     * @see #getHeaderField(String) for header field resolution
     */
    public long getHeaderFieldDate(String name, long defaultValue) {
        final String value = getHeaderField(name);
        if (value == null) return defaultValue;
        try {
            final Date date;
            synchronized (RFC1123_DATE_FORMAT) {
                date = RFC1123_DATE_FORMAT.parse(value);
            }
            if (date == null) {
                return defaultValue;
            }
            return date.getTime();
        } catch (ParseException ex) {
            return defaultValue;
        }
    }

    /**
     * @return headers returned by the server
     */
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    /**
     * A shortcut to check for successful status codes and throw exception in case of non-2xx status codes.
     * <br>
     * In many cases you will call {@link com.goebl.david.Request#ensureSuccess()} instead of this method.
     * But there might be cases where you want to inspect the response-object first (check header values) and
     * then have a short exit where the response-code is not suitable for further normal processing.
     */
    public void ensureSuccess() {
        if (!isSuccess()) {
            throw new WebbException("Request failed: " + statusCode + " " + statusMessage, this);
        }
    }
}
