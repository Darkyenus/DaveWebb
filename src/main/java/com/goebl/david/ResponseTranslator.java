package com.goebl.david;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Interface for translating generic {@link InputStream} responses into useful objects.
 */
public interface ResponseTranslator<Type> {

    /**
     * Decode all data in InputStream into usable object.
     * Note that this will be called without any regard to response code,
     * so this will have to process success and error responses!
     *
     * This method MUST be thread safe and work correctly when invoked from ANY thread!!!
     *
     * @param in stream, never null. Doesn't have to be closed.
     */
    Type decode(Response<Type> response, InputStream in) throws Exception;

    ResponseTranslator<String> STRING_TRANSLATOR = new ResponseTranslator<String>() {
        public String decode(Response<String> response, InputStream in) throws Exception {
            String encoding = WebbConst.UTF8;
            final String[] parts = response.getContentType().replaceAll("\\s", "").split(";");
            for (String part : parts) {
                final String prefix = "charset=";
                if (part.startsWith(prefix)) {
                    final String charset = part.substring(prefix.length());
                    if (Charset.isSupported(charset)) {
                        encoding = charset;
                        break;
                    }
                }
            }

            final InputStreamReader reader = new InputStreamReader(in, encoding);
            final StringBuilder sb = new StringBuilder(1024);
            final char[] buffer = new char[512];
            int read;
            while ((read = reader.read(buffer)) > 0) {
                sb.append(buffer, 0, read);
            }

            return sb.toString();
        }
    };
}
