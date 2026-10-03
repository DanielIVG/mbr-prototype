// Params.java
package org.example.api;

import io.javalin.http.Context;

final class Params {

    private Params() {}

    static Long id(Context ctx) {
        return Long.parseLong(ctx.pathParam("id"));
    }

    static Long requiredLong(Context ctx, String name) {
        String value = ctx.queryParam(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("El parámetro " + name + " es obligatorio");
        }
        return Long.parseLong(value);
    }
}