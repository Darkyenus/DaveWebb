package com.goebl.david;

/**
 *
 */
public interface ResponseCallback<Type> {

    void success(Response<Type> response);

    void failure(WebbException exception);
}
