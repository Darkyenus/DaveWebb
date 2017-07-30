package com.darkyen.dave;

/**
 *
 */
public interface ResponseCallback<Type> {

    void success(Response<Type> response);

    void failure(WebbException exception);
}
