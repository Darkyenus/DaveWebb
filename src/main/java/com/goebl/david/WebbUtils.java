package com.goebl.david;

import java.io.*;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
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
     * Instance of a <code>DateFormat</code> for RFC1123 compliant dates.
     * <br>
     * This DateFormat is not thread-safe, so synchronize on it when using!
     * <br>
     * If you have to deal with dates in this format with JavaScript, it's easy, because the JavaScript
     * Date object has a constructor for strings formatted this way.
     */
    static final SimpleDateFormat RFC1123_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    static {
        RFC1123_DATE_FORMAT.setLenient(false);
        RFC1123_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    static String urlEncode(String value) {
        try {
            return URLEncoder.encode(value, WebbConst.UTF8);
        } catch (UnsupportedEncodingException e) {
            return value;
        }
    }

    static void addRequestProperties(URLConnection connection, Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            addRequestProperty(connection, entry.getKey(), entry.getValue());
        }
    }

    static void addRequestProperty(URLConnection connection, String name, Object value) {
        if (name == null || name.length() == 0 || value == null) {
            throw new IllegalArgumentException("name and value must not be empty");
        }

        String valueAsString;
        if (value instanceof Date) {
            synchronized (RFC1123_DATE_FORMAT) {
                valueAsString = RFC1123_DATE_FORMAT.format((Date) value);
            }
        } else if (value instanceof Calendar) {
            synchronized (RFC1123_DATE_FORMAT) {
                valueAsString = RFC1123_DATE_FORMAT.format(((Calendar) value).getTime());
            }
        } else {
            valueAsString = value.toString();
        }

        connection.addRequestProperty(name, valueAsString);
    }

    static void ensureRequestProperty(URLConnection connection, String name, Object value) {
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
