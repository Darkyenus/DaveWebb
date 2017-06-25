package com.goebl.david;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

/**
 * Static utility method and tools for HTTP traffic parsing and encoding.
 *
 * @author hgoebl
 */
final class WebbUtils {

    private WebbUtils() {}

    /**
     * Convert a Map to a query string.
     * @param values the map with the values
     *               <code>null</code> will be encoded as empty string, all other objects are converted to
     *               String by calling its <code>toString()</code> method.
     * @return e.g. "key1=value&amp;key2=&amp;email=max%40example.com"
     */
    static String queryString(Map<String, Object> values) {
        StringBuilder sbuf = new StringBuilder();
        String separator = "";

        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object entryValue = entry.getValue();
            if (entryValue instanceof Object[]) {
                for (Object value : (Object[]) entryValue) {
                    appendParam(sbuf, separator, entry.getKey(), value);
                    separator = "&";
                }
            } else if (entryValue instanceof Iterable) {
                for (Object multiValue : (Iterable) entryValue) {
                    appendParam(sbuf, separator, entry.getKey(), multiValue);
                    separator = "&";
                }
            } else {
                appendParam(sbuf, separator, entry.getKey(), entryValue);
                separator = "&";
            }
        }

        return sbuf.toString();
    }

    private static void appendParam(StringBuilder sbuf, String separator, String entryKey, Object value) {
        String sValue = value == null ? "" : String.valueOf(value);
        sbuf.append(separator);
        sbuf.append(urlEncode(entryKey));
        sbuf.append('=');
        sbuf.append(urlEncode(sValue));
    }

    /**
     * Read an <code>InputStream</code> into <code>byte[]</code> until EOF.
     * <br>
     * Does not close the InputStream!
     *
     * @param is the stream to read the bytes from
     * @return all read bytes as an array
     * @throws IOException when read or write operation fails
     */
    static byte[] readBytes(InputStream is) throws IOException {
        if (is == null) {
            return null;
        }
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        copyStream(is, byteOut);
        return byteOut.toByteArray();
    }

    /**
     * Copy complete content of <code>InputStream</code> to <code>OutputStream</code> until EOF.
     * <br>
     * Does not close the InputStream nor OutputStream!
     *
     * @param input the stream to read the bytes from
     * @param output the stream to write the bytes to
     * @throws IOException when read or write operation fails
     */
    static void copyStream(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = new byte[1024];
        int count;
        while ((count = input.read(buffer)) != -1) {
            output.write(buffer, 0, count);
        }
    }

    /**
     * Creates a new instance of a <code>DateFormat</code> for RFC1123 compliant dates.
     * <br>
     * Should be stored for later use but be aware that this DateFormat is not Thread-safe!
     * <br>
     * If you have to deal with dates in this format with JavaScript, it's easy, because the JavaScript
     * Date object has a constructor for strings formatted this way.
     * @return a new instance
     */
    static DateFormat getRfc1123DateFormat() {
        DateFormat format = new SimpleDateFormat(
                "EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        return format;
    }

    static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, WebbConst.UTF8);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    static void addRequestProperties(HttpURLConnection connection, Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            addRequestProperty(connection, entry.getKey(), entry.getValue());
        }
    }

    static void addRequestProperty(HttpURLConnection connection, String name, Object value) {
        if (name == null || name.length() == 0 || value == null) {
            throw new IllegalArgumentException("name and value must not be empty");
        }

        String valueAsString;
        if (value instanceof Date) {
            valueAsString = getRfc1123DateFormat().format((Date) value);
        } else if (value instanceof Calendar) {
            valueAsString = getRfc1123DateFormat().format(((Calendar) value).getTime());
        } else {
            valueAsString = value.toString();
        }

        connection.addRequestProperty(name, valueAsString);
    }

    static void ensureRequestProperty(HttpURLConnection connection, String name, Object value) {
        if (!connection.getRequestProperties().containsKey(name)) {
            addRequestProperty(connection, name, value);
        }
    }

    static byte[] gzip(byte[] input) {
        GZIPOutputStream gzipOS = null;
        try {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            gzipOS = new GZIPOutputStream(byteArrayOS);
            gzipOS.write(input);
            gzipOS.close();
            gzipOS = null;
            return byteArrayOS.toByteArray();
        } catch (Exception e) {
            throw new WebbException(e);
        } finally {
            if (gzipOS != null) {
                try { gzipOS.close(); } catch (Exception ignored) {}
            }
        }
    }

    static InputStream decodeStream(String contentEncoding, InputStream inputStream) throws IOException {
        if (contentEncoding == null || "identity".equalsIgnoreCase(contentEncoding)) {
            return inputStream;
        }
        if ("gzip".equalsIgnoreCase(contentEncoding)) {
            return new GZIPInputStream(inputStream);
        }
        if ("deflate".equalsIgnoreCase(contentEncoding)) {
            return new InflaterInputStream(inputStream, new Inflater(false), 512);
        }
        throw new WebbException("unsupported content-encoding: " + contentEncoding);
    }

    static void closeQuietly(Closeable closeable) {
        if (closeable == null) return;
        try {
            closeable.close();
        } catch (IOException ignored) {}
    }
}
