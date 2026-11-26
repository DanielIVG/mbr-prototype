package org.example.client;

public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(String message) {
        this(message, 0);
    }

    public ApiException(String message, int status) {
        super(message);
        this.status = status;
    }

    public ApiException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    /** Código HTTP de la respuesta (0 si el error no vino de una respuesta). */
    public int status() {
        return status;
    }
}
