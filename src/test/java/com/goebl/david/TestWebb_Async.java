package com.goebl.david;

import java.util.concurrent.CountDownLatch;

/**
 *
 */
public class TestWebb_Async extends AbstractTestWebb {

    public void testDefaultAsync() throws InterruptedException {
        // Default async isn't async, but that is not a problem

        final CountDownLatch latch = new CountDownLatch(1);

        webb.get("/ping").executeString(new ResponseCallback<String>() {
            public void success(Response<String> response) {
                assertTrue(response.isSuccess());
                assertEquals("pong", response.getBody());
                latch.countDown();
            }

            public void failure(WebbException exception) {
                fail("Expected success, got: "+exception);
                latch.countDown();
            }
        });

        latch.await();
    }

    public void testThreadedAsync() throws InterruptedException {
        // Default async isn't async, but that is not a problem

        final ExecutionStrategy.Async asyncStrategy = new ExecutionStrategy.Async(1);
        webb.setExecutionStrategy(asyncStrategy);

        final CountDownLatch latch = new CountDownLatch(1);

        final long beforeRequest = System.currentTimeMillis();
        webb.get("/read-timeout").executeString(new ResponseCallback<String>() {
            public void success(Response<String> response) {
                assertTrue(response.isSuccess());
                assertEquals("long-running operations result", response.getBody());
                latch.countDown();
            }

            public void failure(WebbException exception) {
                fail("Expected success, got: "+exception);
                latch.countDown();
            }
        });

        final long afterRequest = System.currentTimeMillis();
        asyncStrategy.shutdown(true);

        latch.await();
        final long afterResponse = System.currentTimeMillis();

        // These timings may fail on very slow machine
        assertTrue("Async request took suspiciously long time",afterRequest - beforeRequest < 100);
        assertTrue("Async response took suspiciously short time",afterResponse - beforeRequest > 500);
    }
}
