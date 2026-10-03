// ConflictException.java
package org.example.service;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}