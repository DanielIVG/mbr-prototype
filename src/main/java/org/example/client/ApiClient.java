package org.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.DeviceDto;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ApiClient {

    private final String baseUrl;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<DeviceDto> listDevices() {
        var response = send(request("/devices").GET().build());
        return read(response.body(), new TypeReference<List<DeviceDto>>() {});
    }

    public DeviceDto createDevice(String name, String intendedUse, String misuse) {
        var body = new DeviceDto(null, name, intendedUse, misuse, true);
        var response = send(request("/devices")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(write(body)))
                .build());
        return read(response.body(), new TypeReference<DeviceDto>() {});
    }

    public void deactivateDevice(long id) {
        send(request("/devices/" + id + "/deactivate")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path));
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ApiException(errorMessage(response));
            }
            return response;
        } catch (IOException e) {
            throw new ApiException("No se pudo conectar con la API local: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException("Operación interrumpida", e);
        }
    }

    private String errorMessage(HttpResponse<String> response) {
        try {
            var node = mapper.readTree(response.body());
            if (node.hasNonNull("error")) {
                return node.get("error").asText();
            }
        } catch (IOException ignored) {
            // se usa el mensaje genérico
        }
        return "Error " + response.statusCode();
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new ApiException("Respuesta inválida de la API", e);
        }
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new ApiException("No se pudo preparar la petición", e);
        }
    }
}