package com.darkyen.dave;

import java.io.InputStream;

/**
 *
 */
public interface BodyStreamProvider<Stream extends InputStream> {
    /**
     * Called when stream is needed to write to the server. May be called again
     * after {@link #destroyStream(InputStream)}, if the request failed and must be performed again.
     */
    Stream createStream() throws Exception;

    /**
     * Called with stream returned by createStream to get the size of stream. This amount (in bytes) must be correct.
     * If it is not possible to tell, return -1.
     */
    long payloadSize(Stream forStream);

    /**
     * Called always after createStream with previously returned stream. Close the stream here, if needed.
     */
    void destroyStream(Stream usedStream);
}
