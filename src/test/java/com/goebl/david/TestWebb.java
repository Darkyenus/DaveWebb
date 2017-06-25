package com.goebl.david;


import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class TestWebb extends AbstractTestWebb {

    public void testMisc() throws Exception {
        Request request = webb
                .get("/ping")
                .useCaches(true);

        assertEquals(HttpMethod.GET, request.method);
        assertEquals(true, request.useCaches);

        Response<String> response = request.executeString();

        assertTrue(response.isSuccess());
        assertEquals("pong", response.getBody());

        assertNotNull(response.getStatusMessage());
        assertNotNull(response.getStatusCode());

        assertSame(request, response.getRequest());
    }

    public void testIgnoreBaseUri() throws Exception {
        webb.get("http://www.goebl.com/robots.txt").ensureSuccess().execute();
    }

    public void testSimpleGetText() throws Exception {
        Response<String> response = webb
                .get("/simple.txt")
                .param("p1", SIMPLE_ASCII)
                .param("p2", COMPLEX_UTF8)
                .executeString();

        assertEquals(200, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals(HTTP_MESSAGE_OK, response.getStatusMessage());
        assertEquals("HTTP/1.1 200 OK", response.getStatusLine());

        assertEquals(SIMPLE_ASCII + ", " + COMPLEX_UTF8, response.getBody());
        assertEquals(WebbConst.MIME_TEXT_PLAIN, response.getContentType());
    }

    public void testSimplePostText() throws Exception {
        Response<String> response = webb
                .post("/simple.txt")
                .param("p1", SIMPLE_ASCII)
                .param("p2", COMPLEX_UTF8)
                .executeString();

        assertEquals(200, response.getStatusCode());
        assertEquals(HTTP_MESSAGE_OK, response.getStatusMessage());
        assertEquals(SIMPLE_ASCII + ", " + COMPLEX_UTF8, response.getBody());
        assertTrue(response.getContentType().startsWith(WebbConst.MIME_TEXT_PLAIN));
    }

    public void testEchoPostText() throws Exception {
        String expected = SIMPLE_ASCII + ", " + COMPLEX_UTF8;
        Response<String> response = webb
                .post("/echoText")
                .body(expected)
                .executeString();

        assertEquals(200, response.getStatusCode());
        assertEquals(HTTP_MESSAGE_OK, response.getStatusMessage());
        assertEquals(expected, response.getBody());
        assertTrue(response.getContentType().startsWith(WebbConst.MIME_TEXT_PLAIN));
    }

    public void testSimpleDelete() throws Exception {

        Response<Void> response = webb
                .delete("/simple")
                .execute();

        assertEquals(204, response.getStatusCode());
        assertTrue(response.isSuccess());
        assertEquals("No Content", response.getStatusMessage());
    }

    public void testNoContent() throws Exception {

        Response<Void> responseAsVoid = webb
                .get("/no-content")
                .execute();

        assertEquals(204, responseAsVoid.getStatusCode());
        assertTrue(responseAsVoid.isSuccess());
        assertEquals("No Content", responseAsVoid.getStatusMessage());

        Response<String> responseAsString = webb
                .get("/no-content")
                .executeString();

        assertEquals(204, responseAsString.getStatusCode());
        assertTrue(responseAsString.isSuccess());
        assertEquals("No Content", responseAsString.getStatusMessage());
        assertEquals("", responseAsString.getBody());
    }

    public void testParameterTypes() throws Exception {
        Response<String> response = webb
                .get("/parameter-types")
                .param("string", SIMPLE_ASCII)
                .param("number", 815)
                .param("number", 4711) // test overwrite feature of multiple calls with same name
                .param("null", null)
                .param("empty", "")
                .executeString();

        assertEquals(204, response.getStatusCode());
    }

    public void testParameterTypesWithMap() throws Exception {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("string", SIMPLE_ASCII);
        params.put("number", 4711);
        params.put("null", null);
        params.put("empty", "");
        Response<String> response = webb
                .get("/parameter-types")
                .params(params)
                .executeString();

        assertEquals(204, response.getStatusCode());
    }

    public void testMultiValuesParameterArray() throws Exception {
        Object values = new Object[]{"abc", 1, true, "abc@abc.com"};
        Response<String> response = webb
                .get("/multiple-valued-parameter")
                .param("m", values)
                .executeString();

        assertEquals(204, response.getStatusCode());
    }

    public void testMultiValuesParameterIterable() throws Exception {
        Object values = Arrays.asList("abc", 1, true, "abc@abc.com");
        Response<String> response = webb
                .get("/multiple-valued-parameter")
                .param("m", values)
                .executeString();

        assertEquals(204, response.getStatusCode());
    }

    public void testMultiValuesParameterSimple() throws Exception {
        Response<String> response = webb
                .get("/multiple-valued-parameter")
                .multipleValues()
                .param("m", "abc")
                .param("m", 1)
                .param("m", true)
                .param("m", "abc@abc.com")
                .executeString();

        assertEquals(204, response.getStatusCode());
    }

    public void testHeadersIn() throws Exception {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.set(2013, Calendar.NOVEMBER, 24, 23, 59, 33);

        Response<Void> response = webb
                .get("/headers/in")
                .header("x-test-string", SIMPLE_ASCII)
                .header("x-test-int", 4711)
                .header("x-test-calendar", cal)
                .header("x-test-date", cal.getTime())
                .param(WebbConst.HDR_USER_AGENT, USER_AGENT)
                .execute();

        assertEquals(200, response.getStatusCode());
    }

    public void testHeadersOut() throws Exception {

        Response<Void> response = webb
                .get("/headers/out")
                .execute();
        long nowMoreOrLess = System.currentTimeMillis();

        assertEquals(200, response.getStatusCode());
        assertEquals(4711, response.getHeaderFieldInt("x-test-int", 0));
        long serverTime = response.getHeaderFieldDate("x-test-datum", 0L);

        assertTrue(Math.abs(serverTime - nowMoreOrLess) < 5000);

        serverTime = response.getDate();
        assertTrue(Math.abs(serverTime - nowMoreOrLess) < 5000);

        assertEquals(SIMPLE_ASCII, response.getHeaderField("x-test-string"));
    }

    public void testHeaderExpires() throws Exception {

        long offset = 3600 * 1000;
        Response<Void> response = webb
                .get("/headers/expires")
                .param("offset", offset)
                .execute();

        assertEquals(200, response.getStatusCode());
        long expiresRaw = response.getHeaderFieldDate("Expires", 0L);
        long expires = response.getExpiration();

        // <10 seconds time drift is ok
        long delta = expires - offset - System.currentTimeMillis();
        if (Math.abs(delta) > 10000) {
            fail("expires / offset mismatch: " + expires + " / " + offset + " delta=" + delta);
        }

        assertEquals(expiresRaw, expires);
    }

    public void testIfModifiedSince() throws Exception {

        long lastModified = System.currentTimeMillis() - 10000; // resource was modified 10 seconds ago

        // we ask if it was modified earlier than 100 seconds ago => yes!
        Response<Void> response = webb
                .get("/headers/if-modified-since")
                .ifModifiedSince(lastModified - 100000)
                .param("lastModified", lastModified)
                .execute();

        assertEquals(200, response.getStatusCode());

        // we ask if it was modified earlier than 5 seconds ago => no!
        response = webb
                .get("/headers/if-modified-since")
                .ifModifiedSince(lastModified + 5000)
                .param("lastModified", lastModified)
                .execute();

        assertEquals(304, response.getStatusCode());
    }

    public void testLastModified() throws Exception {

        long lastModified = (System.currentTimeMillis() / 1000) * 1000L;

        Response<Void> response = webb
                .get("/headers/last-modified")
                .param("lastModified", lastModified)
                .execute();

        assertEquals(200, response.getStatusCode());
        assertEquals(lastModified, response.getLastModified());
    }

    public void testEnsureSuccess() throws Exception {
        String result = webb.get("/ping").ensureSuccess().executeString().getBody();
        assertEquals("pong", result);
    }

    // should be moved to TestRequest
    public void testGetUri() throws Exception {
        final Request request = new Webb("http://example.com").get("/simple.txt");

        assertEquals("http://example.com/simple.txt", request.getUri());
    }
}
