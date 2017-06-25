package com.goebl.david;

import com.esotericsoftware.jsonbeans.JsonValue;

public class TestWebb_ErrorCases extends AbstractTestWebb {

    public void testGetWithBody() throws Exception {
        try {
            webb.get("/does_not_exist").body("some text").execute();
            fail();
        } catch (IllegalStateException expected) {
            // body with get is not allowed
        }
    }

    public void testDeleteWithBody() throws Exception {
        try {
            webb.delete("/does_not_exist").body("some text").execute();
            fail();
        } catch (IllegalStateException expected) {
            // body with get is not allowed
        }
    }

    public void testUriNull() throws Exception {
        try {
            webb.get(null).execute();
            fail();
        } catch (IllegalArgumentException expected) {
            // body with get is not allowed
        }
    }

    public void testError404NoContent() throws Exception {
        Response<String> response = webb
                .get("/error/404")
                .executeString();

        assertFalse(response.isSuccess());
        assertEquals(404, response.getStatusCode());
        assertEquals("Not Found", response.getStatusMessage());
        assertEquals(String.class, response.getBody().getClass());
    }

    public void testError400NoContent() throws Exception {
        Response<String> response = webb
                .get("/error/400/no-content")
                .executeString();

        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertEquals("Bad Request", response.getBody());
    }

    public void testError400WithContent() throws Exception {
        Response<JsonValue> response = webb
                .get("/error/400/with-content")
                .execute(JSON_TRANSLATOR);

        assertFalse(response.isSuccess());
        assertEquals(400, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(JsonValue.class, response.getBody().getClass());
        assertNotNull(response.getBody().getString("msg", null));
    }

    public void testPostError500WithContent_JSON() throws Exception {
        Response<JsonValue> response = webb
                .post("/error/500/with-content")
                .bodyJson("{\"arbitrary\":\"This is some content\"}")
                .execute(JSON_TRANSLATOR);

        assertFalse(response.isSuccess());
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(JsonValue.class, response.getBody().getClass());
        assertEquals("an error has occurred", response.getBody().getString("msg", null));
    }

    public void testPostError500WithContent_String() throws Exception {
        Response<String> response = webb
                .post("/error/500/with-content")
                .body("This is some content")
                .executeString();

        assertFalse(response.isSuccess());
        assertEquals(500, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(String.class, response.getBody().getClass());
        String error = response.getBody();
        assertTrue(error.contains("an error has occurred"));
    }

    public void testEnsureSuccessFailedWithContent() throws Exception {
        try {
            //noinspection unused
            JsonValue result = webb
                    .get("/error/500/with-content")
                    .ensureSuccess()
                    .execute(JSON_TRANSLATOR)
                    .getBody();
            fail("should throw exception");
        } catch (WebbException expected) {
            Response response = expected.getResponse();
            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertEquals(500, response.getStatusCode());
            assertNotNull(response.getBody());
            assertEquals(JsonValue.class, response.getBody().getClass());
            JsonValue errorObject = (JsonValue) response.getBody();
            assertEquals("an error has occurred", errorObject.getString("msg"));
        }
    }

    public void testEnsureSuccessFailedNoContent() throws Exception {
        try {
            //noinspection unused
            String result = webb
                    .get("/error/500/no-content")
                    .ensureSuccess()
                    .executeString()
                    .getBody();
            fail("should throw exception");
        } catch (WebbException expected) {
            Response<String> response = expected.getResponse();
            assertNotNull(response);
            assertFalse(response.isSuccess());
            assertEquals(500, response.getStatusCode());

            final String expectedString = "Internal Server Error";
            final String gotString = response.getBody();
            assertEquals(expectedString, gotString);
        }
    }

}
