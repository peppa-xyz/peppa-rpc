package com.peppa.exceptions;

/**
 * @author: peppa
 * @create: 2024-05-15 13:25
 **/
public class DiscoveryException extends RuntimeException{

    public DiscoveryException() {
    }

    public DiscoveryException(String message) {
        super(message);
    }

    public DiscoveryException(Throwable cause) {
        super(cause);
    }
}