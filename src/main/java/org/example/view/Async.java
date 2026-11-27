package org.example.view;

import javafx.concurrent.Task;

import java.util.function.Consumer;
import java.util.function.Supplier;

/** Ejecuta llamadas a la API fuera del hilo de la interfaz; los resultados vuelven a ese hilo. */
final class Async {

    private Async() {
    }

    static <T> void run(Supplier<T> work, Consumer<T> onSuccess, Consumer<String> onError) {
        Task<T> task = new Task<>() {
            @Override
            protected T call() {
                return work.get();
            }
        };
        task.setOnSucceeded(e -> onSuccess.accept(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            onError.accept(ex == null || ex.getMessage() == null ? "Error desconocido" : ex.getMessage());
        });
        Thread thread = new Thread(task, "api-call");
        thread.setDaemon(true);
        thread.start();
    }
}
