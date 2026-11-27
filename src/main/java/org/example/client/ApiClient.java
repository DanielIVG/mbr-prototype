package org.example.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.api.AssessmentDtos;
import org.example.api.ComponentDto;
import org.example.api.CreateChainRequest;
import org.example.api.RiskChainDto;
import org.example.api.DeviceDto;
import org.example.api.BehaviorDtos;
import org.example.api.DashboardDtos;
import org.example.api.TraceabilityDtos;
import org.example.api.ComponentLibraryDtos;
import org.example.api.FtaDtos;
import org.example.api.FmeaDtos;
import org.example.api.FunctionDto;
import org.example.api.InteractionDto;

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

    public DeviceDto updateDevice(long id, String name, String intendedUse, String misuse) {
        var body = new DeviceDto(id, name, intendedUse, misuse, true);
        var response = send(request("/devices/" + id)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(write(body)))
                .build());
        return read(response.body(), new TypeReference<DeviceDto>() {});
    }

    public void deactivateDevice(long id) {
        send(request("/devices/" + id + "/deactivate")
                .POST(HttpRequest.BodyPublishers.noBody())
                .build());
    }

    // ------------------------------------------------------------ estructura del dispositivo

    public List<ComponentDto> listComponents(long deviceId) {
        var response = send(request("/components?deviceId=" + deviceId).GET().build());
        return read(response.body(), new TypeReference<List<ComponentDto>>() {});
    }

    public ComponentDto createComponent(ComponentDto body) {
        return read(sendJson("/components", "POST", body).body(), new TypeReference<ComponentDto>() {});
    }

    public ComponentDto updateComponent(long id, ComponentDto body) {
        return read(sendJson("/components/" + id, "PUT", body).body(), new TypeReference<ComponentDto>() {});
    }

    public void deactivateComponent(long id) {
        deactivate("/components/" + id + "/deactivate");
    }

    public List<FunctionDto> listFunctions(long deviceId) {
        var response = send(request("/functions?deviceId=" + deviceId).GET().build());
        return read(response.body(), new TypeReference<List<FunctionDto>>() {});
    }

    public FunctionDto createFunction(FunctionDto body) {
        return read(sendJson("/functions", "POST", body).body(), new TypeReference<FunctionDto>() {});
    }

    public FunctionDto updateFunction(long id, FunctionDto body) {
        return read(sendJson("/functions/" + id, "PUT", body).body(), new TypeReference<FunctionDto>() {});
    }

    public void deactivateFunction(long id) {
        deactivate("/functions/" + id + "/deactivate");
    }

    public List<InteractionDto> listInteractions(long deviceId) {
        var response = send(request("/interactions?deviceId=" + deviceId).GET().build());
        return read(response.body(), new TypeReference<List<InteractionDto>>() {});
    }

    public InteractionDto createInteraction(InteractionDto body) {
        return read(sendJson("/interactions", "POST", body).body(), new TypeReference<InteractionDto>() {});
    }

    public InteractionDto updateInteraction(long id, InteractionDto body) {
        return read(sendJson("/interactions/" + id, "PUT", body).body(), new TypeReference<InteractionDto>() {});
    }

    public void deactivateInteraction(long id) {
        deactivate("/interactions/" + id + "/deactivate");
    }

    // ------------------------------------------------------------ cadenas de riesgo

    public RiskChainDto createChain(CreateChainRequest body) {
        return read(sendJson("/risk-chains", "POST", body).body(), new TypeReference<RiskChainDto>() {});
    }

    public List<RiskChainDto> listChains() {
        var response = send(request("/risk-chains").GET().build());
        return read(response.body(), new TypeReference<List<RiskChainDto>>() {});
    }

    public List<RiskChainDto.SequenceDto> listSequence(long hazardId) {
        var response = send(request("/hazards/" + hazardId + "/sequences").GET().build());
        return read(response.body(), new TypeReference<List<RiskChainDto.SequenceDto>>() {});
    }

    // ------------------------------------------------------------ ISO 14971: criterios y estimación

    /** Devuelve null si los criterios todavía no se han definido. */
    public AssessmentDtos.CriteriaDto getCriteria() {
        try {
            return read(send(request("/criteria").GET().build()).body(),
                    new TypeReference<AssessmentDtos.CriteriaDto>() {});
        } catch (ApiException e) {
            if (e.status() == 404) {
                return null;
            }
            throw e;
        }
    }

    public AssessmentDtos.CriteriaDto saveCriteria(AssessmentDtos.CriteriaDto body) {
        return read(sendJson("/criteria", "PUT", body).body(),
                new TypeReference<AssessmentDtos.CriteriaDto>() {});
    }

    public List<AssessmentDtos.EvidenceLevelDto> listEvidenceLevels() {
        return read(send(request("/evidence-levels").GET().build()).body(),
                new TypeReference<List<AssessmentDtos.EvidenceLevelDto>>() {});
    }

    public AssessmentDtos.EstimationDto estimate(long harmId, AssessmentDtos.EstimationRequest body) {
        return read(sendJson("/harms/" + harmId + "/estimations", "POST", body).body(),
                new TypeReference<AssessmentDtos.EstimationDto>() {});
    }

    /** Devuelve null si el daño todavía no tiene estimación. */
    public AssessmentDtos.EstimationDto latestEstimation(long harmId) {
        try {
            return read(send(request("/harms/" + harmId + "/estimation").GET().build()).body(),
                    new TypeReference<AssessmentDtos.EstimationDto>() {});
        } catch (ApiException e) {
            if (e.status() == 404) {
                return null;
            }
            throw e;
        }
    }

    public List<AssessmentDtos.EstimationDto> estimationHistory(long harmId) {
        return read(send(request("/harms/" + harmId + "/estimations").GET().build()).body(),
                new TypeReference<List<AssessmentDtos.EstimationDto>>() {});
    }

    public AssessmentDtos.SensitivityDto sensitivity(long harmId) {
        return read(send(request("/harms/" + harmId + "/sensitivity").GET().build()).body(),
                new TypeReference<AssessmentDtos.SensitivityDto>() {});
    }

    public AssessmentDtos.ControlDto addControl(long harmId, String description, String type) {
        return read(sendJson("/harms/" + harmId + "/controls", "POST",
                new AssessmentDtos.ControlDto(null, harmId, description, type)).body(),
                new TypeReference<AssessmentDtos.ControlDto>() {});
    }

    public List<AssessmentDtos.ControlDto> listControls(long harmId) {
        return read(send(request("/harms/" + harmId + "/controls").GET().build()).body(),
                new TypeReference<List<AssessmentDtos.ControlDto>>() {});
    }

    public AssessmentDtos.VerificationDto addVerification(long controlId, String method,
                                                          boolean implemented, Boolean effective, String evidence) {
        return read(sendJson("/controls/" + controlId + "/verifications", "POST",
                new AssessmentDtos.VerificationDto(null, controlId, method, implemented, effective, null, evidence)).body(),
                new TypeReference<AssessmentDtos.VerificationDto>() {});
    }

    public List<AssessmentDtos.VerificationDto> listVerifications(long controlId) {
        return read(send(request("/controls/" + controlId + "/verifications").GET().build()).body(),
                new TypeReference<List<AssessmentDtos.VerificationDto>>() {});
    }

    // ------------------------------------------------------------ árbol de fallas

    public FtaDtos.FtaDto saveFta(long harmId, FtaDtos.FtaDto body) {
        return read(sendJson("/harms/" + harmId + "/fta", "PUT", body).body(),
                new TypeReference<FtaDtos.FtaDto>() {});
    }

    /** Devuelve null si el daño todavía no tiene árbol. */
    public FtaDtos.FtaDto getFta(long harmId) {
        try {
            return read(send(request("/harms/" + harmId + "/fta").GET().build()).body(),
                    new TypeReference<FtaDtos.FtaDto>() {});
        } catch (ApiException e) {
            if (e.status() == 404) {
                return null;
            }
            throw e;
        }
    }

    // ------------------------------------------------------------ FMEA: modos de falla

    public FmeaDtos.FailureModeDto createFailureMode(FmeaDtos.FailureModeRequest body) {
        return read(sendJson("/failure-modes", "POST", body).body(),
                new TypeReference<FmeaDtos.FailureModeDto>() {});
    }

    private void deactivate(String path) {
        send(request(path).POST(HttpRequest.BodyPublishers.noBody()).build());
    }

    private HttpResponse<String> sendJson(String path, String method, Object body) {
        return send(request(path)
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(write(body)))
                .build());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path));
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new ApiException(errorMessage(response), response.statusCode());
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

    // ------------------------------------------------------------ trazabilidad

    public List<TraceabilityDtos.RequirementDto> listRequirements(long deviceId) {
        var response = send(request("/requirements?deviceId=" + deviceId).GET().build());
        return read(response.body(), new TypeReference<List<TraceabilityDtos.RequirementDto>>() {});
    }

    public TraceabilityDtos.RequirementDto createRequirement(TraceabilityDtos.RequirementRequest body) {
        return read(sendJson("/requirements", "POST", body).body(),
                new TypeReference<TraceabilityDtos.RequirementDto>() {});
    }

    public TraceabilityDtos.RequirementDto updateRequirement(long id, TraceabilityDtos.RequirementRequest body) {
        return read(sendJson("/requirements/" + id, "PUT", body).body(),
                new TypeReference<TraceabilityDtos.RequirementDto>() {});
    }

    public void deactivateRequirement(long id) {
        deactivate("/requirements/" + id + "/deactivate");
    }

    public void linkRequirement(long requirementId, long controlId) {
        sendJson("/requirements/" + requirementId + "/controls", "POST",
                new TraceabilityDtos.LinkRequest(controlId));
    }

    public void unlinkRequirement(long requirementId, long controlId) {
        send(request("/requirements/" + requirementId + "/controls/" + controlId).DELETE().build());
    }

    public List<TraceabilityDtos.ControlDto> listDeviceControls(long deviceId) {
        var response = send(request("/devices/" + deviceId + "/controls").GET().build());
        return read(response.body(), new TypeReference<List<TraceabilityDtos.ControlDto>>() {});
    }

    public TraceabilityDtos.MatrixDto traceability(long deviceId) {
        var response = send(request("/devices/" + deviceId + "/traceability").GET().build());
        return read(response.body(), new TypeReference<TraceabilityDtos.MatrixDto>() {});
    }

    /** Hacia atrás: componentes por los que pasó un daño. */
    public List<TraceabilityDtos.ElementRefDto> componentsOfHarm(long harmId) {
        var response = send(request("/harms/" + harmId + "/components").GET().build());
        return read(response.body(), new TypeReference<List<TraceabilityDtos.ElementRefDto>>() {});
    }

    /** Hacia delante: daños a los que lleva un componente. */
    public List<TraceabilityDtos.HarmRefDto> harmsOfComponent(long componentId) {
        var response = send(request("/components/" + componentId + "/harms").GET().build());
        return read(response.body(), new TypeReference<List<TraceabilityDtos.HarmRefDto>>() {});
    }

    /** Indicios del origen de un daño (null si no fue generado por árbol de comportamiento). */
    public TraceabilityDtos.OriginTraceDto originTrace(long harmId) {
        try {
            var response = send(request("/harms/" + harmId + "/origin-trace").GET().build());
            return read(response.body(), new TypeReference<TraceabilityDtos.OriginTraceDto>() {});
        } catch (ApiException e) {
            if (e.status() == 404) {
                return null;
            }
            throw e;
        }
    }

    public DashboardDtos.DashboardDto rpnDashboard(long deviceId) {
        var response = send(request("/devices/" + deviceId + "/rpn-dashboard").GET().build());
        return read(response.body(), new TypeReference<DashboardDtos.DashboardDto>() {});
    }

    // ------------------------------------------------------------ árbol de comportamiento

    public List<BehaviorDtos.NodeDto> listBehavior(long componentId) {
        var response = send(request("/components/" + componentId + "/behavior").GET().build());
        return read(response.body(), new TypeReference<List<BehaviorDtos.NodeDto>>() {});
    }

    public List<BehaviorDtos.NodeDto> listDeviceBehavior(long deviceId) {
        var response = send(request("/devices/" + deviceId + "/behavior").GET().build());
        return read(response.body(), new TypeReference<List<BehaviorDtos.NodeDto>>() {});
    }

    public BehaviorDtos.NodeDto createBehaviorNode(BehaviorDtos.CreateNodeRequest body) {
        return read(sendJson("/behavior-nodes", "POST", body).body(), new TypeReference<BehaviorDtos.NodeDto>() {});
    }

    public BehaviorDtos.NodeDto updateBehaviorNode(long id, String description) {
        return read(sendJson("/behavior-nodes/" + id, "PUT", new BehaviorDtos.UpdateNodeRequest(description)).body(),
                new TypeReference<BehaviorDtos.NodeDto>() {});
    }

    public void deactivateBehaviorNode(long id) {
        deactivate("/behavior-nodes/" + id + "/deactivate");
    }

    public BehaviorDtos.FailureModeRef linkFailureModeToBehaviorNode(long failureModeId, long behaviorNodeId) {
        return read(sendJson("/failure-modes/link", "POST",
                        new BehaviorDtos.LinkFailureModeRequest(failureModeId, behaviorNodeId)).body(),
                new TypeReference<BehaviorDtos.FailureModeRef>() {});
    }

    public BehaviorDtos.FailureModeRef unlinkFailureModeFromBehaviorNode(long failureModeId) {
        return read(sendJson("/failure-modes/" + failureModeId + "/unlink", "POST", null).body(),
                new TypeReference<BehaviorDtos.FailureModeRef>() {});
    }

    // ------------------------------------------------------------ biblioteca de componentes

    public List<ComponentLibraryDtos.EntryDto> listComponentLibrary() {
        var response = send(request("/component-library").GET().build());
        return read(response.body(), new TypeReference<List<ComponentLibraryDtos.EntryDto>>() {});
    }

    public List<ComponentLibraryDtos.MatchDto> searchComponentLibrary(String query, int limit) {
        String q = java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
        var response = send(request("/component-library/search?q=" + q + "&limit=" + limit).GET().build());
        return read(response.body(), new TypeReference<List<ComponentLibraryDtos.MatchDto>>() {});
    }
}
