package com.darkyen.dave;

import com.esotericsoftware.jsonbeans.JsonValue;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;

public class TestWebb_Ssl extends AbstractTestWebb {

    @Override
    String uri() {
        return null;
    }

    public void testHttpsValidCertificate() throws Exception {
        Response<JsonValue> response = webb
                .get("https://www.googleapis.com/oauth2/v1/certs")
                .ensureSuccess()
                .execute(JSON_TRANSLATOR);

        assertEquals(200, response.getStatusCode());
    }

    public void testHttpsInvalidCertificate() throws Exception {
        try {
            webb.get("https://tv.eurosport.com/").ensureSuccess().executeString();
            fail();
        } catch (WebbException e) {
            Class expected = isAndroid() ? IOException.class : SSLHandshakeException.class;
            assertEquals(expected, e.getCause().getClass());
        }
    }

    // @Ignore // TODO since upgrade to Debian 7 this test fails -> fix it!
    public void ignoreHttpsHostnameIgnore() throws Exception {
        webb.setHostnameVerifier(new TrustingHostnameVerifier());

        // www.wellcrafted.de has same IP as www.goebl.com, but certificate is for www.goebl.com
        Response<Void> response = webb.get("https://localhost:13003/").execute();
        assertTrue(response.isSuccess());
    }

    public void testHttpsInvalidCertificateAndHostnameIgnore() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[] { new AlwaysTrustManager() };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

        webb.setSSLSocketFactory(sslContext.getSocketFactory());
        webb.setHostnameVerifier(new TrustingHostnameVerifier());

        Response<Void> response = webb.get("https://localhost:13003/").execute();
        assertTrue(response.isSuccess() || response.getStatusCode() == 301);
    }

    private static class TrustingHostnameVerifier implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    private static class AlwaysTrustManager implements X509TrustManager {
        public void checkClientTrusted(X509Certificate[] arg0, String arg1) { }
        public void checkServerTrusted(X509Certificate[] arg0, String arg1) { }
        public X509Certificate[] getAcceptedIssuers() { return null; }
    }
}
