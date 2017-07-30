package com.darkyen.dave;

public class TestWebb_Redirect extends AbstractTestWebb {

    private static final String TARGET_RESPONSE_TEXT = "redirected to target";
    private static final String TARGET_LOCATION = "/redirect/target";

    public void testTargetPage() throws Exception {
        // first validate that the redirected page works as expected
        assertEquals(TARGET_RESPONSE_TEXT, webb.get(TARGET_LOCATION).ensureSuccess().executeString().getBody());
    }

    public void testMovedPermanentlyNull() throws Exception {
        // without any options redirect should behave like HUC (followRedirects = ON)
        assertEquals(TARGET_RESPONSE_TEXT, webb
                .get("/redirect/301")
                .ensureSuccess()
                .executeString()
                .getBody());
    }

    public void testMovedPermanentlyAutomatic() throws Exception {
        webb.setFollowRedirects(false);

        Response<String> response = webb
                .get("/redirect/301")
                .followRedirects(true)
                .executeString();

        assertTrue(response.isSuccess());
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(TARGET_RESPONSE_TEXT, response.getBody());
    }

    public void testMovedPermanentlyManual() throws Exception {
        webb.setFollowRedirects(false);

        Response<String> response = webb
                .get("/redirect/301")
                .executeString();

        assertFalse(response.isSuccess());
        assertEquals(301, response.getStatusCode());
        assertEquals("<p>Moved Permanently. Redirecting to <a href=\"/redirect/target\">/redirect/target</a></p>", response.getBody());
        String location = response.getHeaderField("Location");
        assertNotNull(location);
        assertTrue(location.endsWith(TARGET_LOCATION));
    }

    public void testPostRedirectGetAutomatic() throws Exception {
        // Java SE: HttpURLConnection should not redirect automatically after POST
        // Android: HttpURLConnection redirects even if it's a POST
        // ... or at least sometimes
        Response<String> response = webb
                .post("/redirect/303")
                .header("x-testcase", "testPostRedirectGetAutomatic")
                .header("Connection", "Close") // not necessary in Kitkat
                .followRedirects(true)
                .body("this is my body")
                .executeString();

        // Behavior of this test is somewhat implementation dependent
        if (response.getStatusCode() == 200) {
            assertTrue(response.isSuccess());
            assertEquals(200, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(TARGET_RESPONSE_TEXT, response.getBody());
        } else {
            assertEquals(303, response.getStatusCode());
            assertNull(response.getBody());
            String location = response.getHeaderField("Location");
            assertNotNull(location);
            assertTrue(location.endsWith(TARGET_LOCATION));
        }
    }

    public void testPostRedirectGetManual() throws Exception {

        // express problem GET with body
        // https://groups.google.com/forum/#!topic/express-js/6nYPXZpzs3U

        Response<String> response = webb
                .post("/redirect/303") // + "?cb=" + System.currentTimeMillis()
                .header("x-testcase", "testPostRedirectGetManual")
                .header("Connection", "Close") // not necessary in Kitkat
                .useCaches(false)
                .followRedirects(false)
                .body("this is my body")
                .executeString();

        assertEquals(303, response.getStatusCode());
        assertEquals("<p>See Other. Redirecting to <a href=\"/redirect/target\">/redirect/target</a></p>", response.getBody());
        String location = response.getHeaderField("Location");
        assertNotNull(location);
        assertTrue(location.endsWith(TARGET_LOCATION));

        // ATTENTION: be careful if you just copy this code snippet!
        // location can be a relative URL. When combined with your setBaseUri(), this could
        // lead to wrong absolute URLs used by webb.
        // There is no danger when Location header returns absolute URL.

        assertEquals(TARGET_RESPONSE_TEXT, webb.get(location).executeString().getBody());
    }

}
