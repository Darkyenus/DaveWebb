package com.goebl.david;

/**
 * Constant values and strings.
 */
public final class WebbConst {
    private WebbConst() {
    }

    public static final String DEFAULT_USER_AGENT = "com.goebl.david.Webb/1.0";

    // MIME types
    public static final String MIME_URLENCODED = "application/x-www-form-urlencoded";
    public static final String MIME_JSON = "application/json";
    public static final String MIME_BINARY = "application/octet-stream";
    public static final String MIME_TEXT_PLAIN = "text/plain";

    // Headers
    public static final String HDR_CONTENT_TYPE = "Content-Type";
    public static final String HDR_CONTENT_ENCODING = "Content-Encoding";
    public static final String HDR_ACCEPT_ENCODING = "Accept-Encoding";
    public static final String HDR_ACCEPT = "Accept";
    public static final String HDR_USER_AGENT = "User-Agent";

    // Private
    static final String UTF8 = "UTF-8";

    static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    static final Class BYTE_ARRAY_CLASS = EMPTY_BYTE_ARRAY.getClass();
    /** Minimal number of bytes the compressed content must be smaller than uncompressed */
    static final int MIN_COMPRESSED_ADVANTAGE = 80;
}
