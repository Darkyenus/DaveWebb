package com.darkyen.dave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Random;

import com.esotericsoftware.jsonbeans.JsonValue;

public class TestWebb_UpDownload extends AbstractTestWebb {
    private static File TEST_FILE;
    private boolean testFileCreated;

    // @BeforeClass
    public static void createTestFile() throws Exception {
        if (TEST_FILE == null) {
            File dir = new File(
                    isAndroid()
                            ? System.getProperty("CACHE_DIR")
                            : System.getProperty("java.io.tmpdir"));
            TEST_FILE = new File(dir, "test-upload.tmp");
        }
        FileOutputStream fos = new FileOutputStream(TEST_FILE);
        byte[] bytes = new byte[1000];
        for (int i = 0; i < bytes.length; ++i) {
            bytes[i] = (byte) 64;
        }
        for (int i = 0; i < 5; ++i) {
            fos.write(bytes);
        }
        fos.flush();
        fos.close();
    }

    // @AfterClass
    public static void unlinkTestFile() {
        TEST_FILE.delete();
    }

    @Override
    protected void tearDown() throws Exception {
        if (testFileCreated) {
            unlinkTestFile();
        }

        super.tearDown();
    }

    public void testUploadFile() throws Exception {
        createTestFile();
        testFileCreated = true;

        Response<Void> response = webb
                .post("/upload?file")
                .body(TEST_FILE)
                .execute();

        assertEquals(201, response.getStatusCode());
    }

    public void testUploadStream() throws Exception {
        createTestFile();
        testFileCreated = true;


        Response<Void> response = webb
                .post("/upload?stream")
                .body(new BodyStreamProvider<FileInputStream>() {

                    public FileInputStream createStream() throws Exception {
                        return new FileInputStream(TEST_FILE);
                    }

                    public long payloadSize(FileInputStream forStream) {
                        return -1;
                    }

                    public void destroyStream(FileInputStream usedStream) {
                        WebbUtils.closeQuietly(usedStream);
                    }
                }, null)
                .execute();

        assertEquals(201, response.getStatusCode());
    }

    public void testUploadCompressedStream() throws Exception {
        createTestFile();
        testFileCreated = true;

        Response<Void> response = webb
                .post("/upload-compressed")
                .compress()
                .body(TEST_FILE)
                .execute();

        assertEquals(201, response.getStatusCode());
    }

    public void testEchoCompressedBytes() throws Exception {
        byte[] payload = new byte[5000];
        new Random().nextBytes(payload);

        Response<byte[]> response = webb
                .post("/echoBin") //force-content-encoding
                .header(WebbConst.HDR_ACCEPT_ENCODING, "gzip")
                .compress()
                .body(payload, null)
                .executeBytes();

        assertEquals(200, response.getStatusCode());
        byte[] echoed = response.getBody();
        assertNotNull(echoed);
        assertArrayEquals(payload, echoed);
    }

    public void testCompressedRandomBytes() throws Exception {
        // random bytes cannot be compressed efficiently, so there should be a fallback to 'identity'
        byte[] payload = new byte[5000];
        new Random().nextBytes(payload);

        Response<byte[]> response = webb
                .post("/echoBin?force-content-encoding=identity")
                .compress()
                .body(payload, null)
                .executeBytes();

        assertEquals(200, response.getStatusCode());
        byte[] echoed = response.getBody();
        assertNotNull(echoed);
        assertArrayEquals(payload, echoed);
    }

    public void testCompressedSqueezableBytes() throws Exception {
        byte[] payload = new byte[5000];
        for (int i = 0; i < payload.length; ++i) {
            payload[i] = (byte) (0xFF & (i / 100));
        }

        Response<byte[]> response = webb
                .post("/echoBin?force-content-encoding=identity")
                .compress()
                .body(payload, null)
                .executeBytes();

        assertEquals(200, response.getStatusCode());
        byte[] echoed = response.getBody();
        assertNotNull(echoed);
        assertTrue(payload.length > echoed.length);

        byte[] gunzip = TestWebbUtils_NoMock.gUnzip(echoed);
        assertArrayEquals(payload, gunzip);
    }

    public void testDownloadGzip() throws Exception {

        Response<JsonValue> response = webb
                .get("/compressed.json")
                .header(WebbConst.HDR_ACCEPT_ENCODING, "gzip")
                .execute(JSON_TRANSLATOR);

        assertEquals(200, response.getStatusCode());
        assertEquals(500, response.getBody().size);
    }

    public void testDownloadDeflate() throws Exception {

        Response<JsonValue> response = webb
                .get("/compressed.json")
                .header(WebbConst.HDR_ACCEPT_ENCODING, "deflate")
                .execute(JSON_TRANSLATOR);

        assertEquals(200, response.getStatusCode());
        assertEquals(500, response.getBody().size);
    }

    public void testDownloadUnknownEncoding() throws Exception {
        Request request = webb
                .get("/compressed.json")
                .header(WebbConst.HDR_ACCEPT_ENCODING, "unknown");

        try {
            //noinspection unused
            Response<byte[]> response = request.executeBytes();
        } catch (WebbException expected) {
            assertTrue(expected.getMessage().matches("^unsupported.*encoding.*$"));
            return;
        }
        fail("unknown encoding not detected");
    }

    public void testEchoBytes() throws Exception {
        byte[] msg = (SIMPLE_ASCII + ", " + COMPLEX_UTF8).getBytes(WebbConst.UTF8);
        Response<byte[]> response = webb
                .post("/echoBin")
                .body(msg, null)
                .executeBytes();

        assertArrayEquals(msg, response.getBody());
    }

    public void testEchoAsStream() throws Exception {
        byte[] msg = (SIMPLE_ASCII + ", " + COMPLEX_UTF8).getBytes(WebbConst.UTF8);
        Response<byte[]> response = webb
                .post("/echoBin")
                .body(msg, null)
                .execute(new ResponseTranslator<byte[]>() {
                    public byte[] decode(Response response, InputStream in) throws Exception {
                        return WebbUtils.readBytes(in);
                    }

                    public byte[] decodeEmptyBody(Response response) throws Exception {
                        return null;
                    }
                });
        assertArrayEquals(msg, response.body);
    }
}
