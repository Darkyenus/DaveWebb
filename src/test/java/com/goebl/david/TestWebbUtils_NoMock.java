package com.goebl.david;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.zip.GZIPInputStream;

public class TestWebbUtils_NoMock extends TestCase {

    public void testQueryString() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        assertEquals("empty map", "", WebbUtils.queryString(map));

        map.put("abc", 123);
        assertEquals("simple single param", "abc=123", WebbUtils.queryString(map));

        map.put("dumb param", Boolean.TRUE);
        assertEquals("uri-encode param", "abc=123&dumb+param=true", WebbUtils.queryString(map));

        map.clear();
        map.put("email", "abc@abc.com");
        assertEquals("uri-encode value", "email=abc%40abc.com", WebbUtils.queryString(map));
    }

    public void testQueryStringMultiValuesObjectArray() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        map.put("m", new Object[]{"abc", 1, true, "abc@abc.com"});
        assertEquals("multi-values", "m=abc&m=1&m=true&m=abc%40abc.com", WebbUtils.queryString(map));
    }

    public void testQueryStringMultiValuesStringArray() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        map.put("m", new String[]{"abc", "1", "true", "abc@abc.com"});
        assertEquals("multi-values", "m=abc&m=1&m=true&m=abc%40abc.com", WebbUtils.queryString(map));
    }

    public void testQueryStringMultiValuesCollection() throws Exception {
        Map<String, Object> map = new LinkedHashMap<String, Object>();

        map.put("m", Arrays.asList("abc", 1, true, "abc@abc.com"));
        assertEquals("multi-values", "m=abc&m=1&m=true&m=abc%40abc.com", WebbUtils.queryString(map));
    }

    public void testUrlEncode() throws Exception {
        assertEquals("", WebbUtils.urlEncode(""));
        assertEquals("normal-ascii", WebbUtils.urlEncode("normal-ascii"));

        // instead of '+' for space '%20' is valid as well; in case of problems adapt test
        assertEquals("Hello%2FWorld+%26+Co.%3F", WebbUtils.urlEncode("Hello/World & Co.?"));
        assertEquals("M%C3%BCnchen+1+Ma%C3%9F+10+%E2%82%AC", WebbUtils.urlEncode("München 1 Maß 10 €"));
    }

    public void testReadBytes() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1024 * 2 + 100; ++i) {
            sb.append(Character.valueOf((char) (i % 256)));
        }
        byte[] input = sb.toString().getBytes("UTF-8");
        ByteArrayInputStream is = new ByteArrayInputStream(input);

        byte[] read = WebbUtils.readBytes(is);
        is.close();

        assertArrayEquals(input, read);

        assertNull("return null when is=null", WebbUtils.readBytes(null));
    }

    public void testAddRequestProperty_valueNull() throws Exception {
        try {
            WebbUtils.addRequestProperty(null, "name1", null);
        } catch (IllegalArgumentException expected) {
            return;
        }
        fail();
    }

    public void testAddRequestProperty_nameNull() throws Exception {
        try {
            WebbUtils.addRequestProperty(null, null, "abc");
        } catch (IllegalArgumentException expected) {
            return;
        }
        fail();
    }

    public void testAddRequestProperty_nameEmpty() throws Exception {
        try {
            WebbUtils.addRequestProperty(null, "", "abc");
        } catch (IllegalArgumentException expected) {
            return;
        }
        fail();
    }

    public void testGetRfc1123DateFormat() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(2013, Calendar.DECEMBER, 24, 23, 59);
        cal.set(Calendar.SECOND, 30);
        cal.set(Calendar.MILLISECOND, 501);
        Date date = cal.getTime();

        String formatted;
        synchronized (WebbUtils.RFC1123_DATE_FORMAT) {
            formatted = WebbUtils.RFC1123_DATE_FORMAT.format(date);
        }

        if (!formatted.matches("^Tue, 24 Dec 2013 23:59:30 UTC(\\+0+:?0{0,2})?$")) {
            fail();
        }
    }

    public void testGzip() throws Exception {
        try {
            WebbUtils.gzip(null);
            fail("should not accept null array");
        } catch (WebbException expected) {
            // good dog!
        }

        byte[] payload = new byte[5000];
        for (int i = 0; i < payload.length; ++i) {
            payload[i] = (byte) (0xFF & (i / 100));
        }

        byte[] gzip = WebbUtils.gzip(payload);

        assertNotNull(gzip);
        assertTrue(payload.length > gzip.length);

        assertArrayEquals(payload, gUnzip(gzip));
    }

    static byte[] gUnzip(byte[] gzip) throws Exception {
        ByteArrayInputStream baIs = new ByteArrayInputStream(gzip);
        GZIPInputStream gzipInputStream = new GZIPInputStream(baIs);
        ByteArrayOutputStream baOs = new ByteArrayOutputStream();
        WebbUtils.copyStream(gzipInputStream, baOs);
        gzipInputStream.close();
        baOs.close();
        return baOs.toByteArray();
    }

    private void assertArrayEquals(byte[] expected, byte[] bytes) {
        assertEquals("array length mismatch", expected.length, bytes.length);
        for (int i = 0; i < expected.length; i++) {
            if (expected[i] != bytes[i]) {
                fail(String.format("array different at index %d expected: %d, is: %d", i, expected[i], bytes[i]));
            }
        }
    }

}
