package com.goebl.david;

import com.esotericsoftware.jsonbeans.JsonReader;
import com.esotericsoftware.jsonbeans.JsonValue;
import junit.framework.TestCase;

import java.io.InputStream;

public abstract class AbstractTestWebb extends TestCase {
    static final String SIMPLE_ASCII = "Hello/World & Co.?";
    static final String COMPLEX_UTF8 = "München 1 Maß 10 €";
    static final String HTTP_MESSAGE_OK = "OK";

    static final String USER_AGENT = "com.goebl.david.Webb/1.0";

    protected Webb webb;

    static boolean isAndroid() {
        return System.getProperty("ANDROID") != null;
    }

    static boolean isEmulator() {
        return System.getProperty("EMULATOR") != null;
    }

    String uri() {
        final String host;
        if (isAndroid()) {
            // Developer-specific values
            if (isEmulator()) {
                host = "10.0.2.2";
            } else {
                host = "192.168.0.147";
            }
        } else {
            host = "localhost";
        }

        return "http://" + host + ":3003";
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        webb = new Webb(uri());
        webb.setDefaultHeader(WebbConst.HDR_USER_AGENT, USER_AGENT);
    }

    static void assertArrayEquals(byte[] expected, byte[] bytes) {
        assertEquals("array length mismatch", expected.length, bytes.length);
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != bytes[i]) {
                fail(String.format("array different at index %d expected: %d, is: %d", i, expected[i], bytes[i]));
            }
        }
    }

    static ResponseTranslator<JsonValue> JSON_TRANSLATOR = new ResponseTranslator<JsonValue>() {
        public JsonValue decode(Response response, InputStream in) throws Exception {
            final String text = ResponseTranslator.STRING_TRANSLATOR.decode(response, in);
            return new JsonReader().parse(text);
        }

        private final JsonValue NULL = new JsonValue(JsonValue.ValueType.nullValue);

        public JsonValue decodeEmptyBody(Response response) throws Exception {
            return NULL;
        }
    };

}
