package com.peppa.exceptions;

/**
 * @author: peppa
 * @create: 2024-05-15 10:31
 **/
public class NetworkException extends RuntimeException{

    public NetworkException() {
    }

    public NetworkException(String message) {
        super(message);
    }

    public NetworkException(Throwable cause) {
        super(cause);
    }
}
