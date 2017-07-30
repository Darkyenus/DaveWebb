package com.darkyen.dave;

/**
 *
 */
public enum HttpMethod {
    GET(false), POST(true), PUT(true), DELETE(false);

    public final boolean canHaveBody;

    HttpMethod(boolean canHaveBody) {
        this.canHaveBody = canHaveBody;
    }
}
