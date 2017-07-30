package com.darkyen.dave;

import java.net.SocketTimeoutException;

public class TestWebb_Timeouts extends AbstractTestWebb {

    public void testConnectTimeoutRequest() throws Exception {
        final Webb webb = new Webb(null);
        try {
            webb.get("http://www.goebl.com/robots.txt").connectTimeout(11).execute();
        } catch (WebbException e) {
            assertEquals(SocketTimeoutException.class, e.getCause().getClass());
        }
    }

    public void testConnectTimeoutGlobal() throws Exception {
        final Webb webb = new Webb(null);
        webb.setConnectTimeout(11);
        try {
            webb.get("http://www.goebl.com/robots.txt").execute();
        } catch (WebbException e) {
            assertEquals(SocketTimeoutException.class, e.getCause().getClass());
        }
    }

    public void testConnectTimeoutRequestOverrulesGlobal() throws Exception {
        final Webb webb = new Webb(null);
        webb.setConnectTimeout(11);
        try {
            webb.get("http://www.goebl.com/robots.txt").connectTimeout(10000).execute();
        } catch (WebbException e) {
            fail("no exception expected (only if server is down), but is: " + e);
        }
    }

    public void testReadTimeoutRequest() throws Exception {
        // the REST api delivers after 500 millis
        webb.get("/read-timeout").readTimeout(800).ensureSuccess().executeString();

        try {
            webb.get("/read-timeout").readTimeout(100).executeString();
        } catch (WebbException e) {
            assertEquals(SocketTimeoutException.class, e.getCause().getClass());
        }
    }

    public void testReadTimeoutGlobal() throws Exception {
        // the REST api delivers after 500 millis
        webb.setReadTimeout(800);
        webb.get("/read-timeout").ensureSuccess().executeString();

        try {
            webb.setReadTimeout(100);
            webb.get("/read-timeout").executeString();
        } catch (WebbException e) {
            assertEquals(SocketTimeoutException.class, e.getCause().getClass());
        } finally {
            webb.setReadTimeout(180000);
        }
    }

    public void testReadTimeoutRequestOverrulesGlobal() throws Exception {
        webb.setReadTimeout(11);
        try {
            webb.get("/read-timeout").readTimeout(1000).executeString();
        } catch (WebbException e) {
            fail("no exception expected (only if server is busy), but is: " + e);
        } finally {
            webb.setReadTimeout(180000);
        }
    }

}
