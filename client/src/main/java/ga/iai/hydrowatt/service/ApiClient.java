package ga.iai.hydrowatt.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP générique vers l'API Django REST HydroWatt.
 * Seul point d'accès réseau du client JavaFX : aucun accès direct à
 * PostgreSQL, conformément au cahier des charges.
 */
public class ApiClient {

    // Adapter cette URL selon l'environnement (voir README du client).
    public static String BASE_URL = "http://127.0.0.1:8000/api";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private static final ObjectMapper MAPPER = JsonMapper.get();

    // ---------------------------------------------------------------
    // Authentification
    // ---------------------------------------------------------------

    public static JsonNode login(String username, String password) throws ApiException, IOException, InterruptedException {
        Map<String, String> body = Map.of("username", username, "password", password);
        return postPublic("/auth/login/", body);
    }

    public static JsonNode verify2fa(String preAuthToken, String code) throws ApiException, IOException, InterruptedException {
        Map<String, String> body = Map.of("pre_auth_token", preAuthToken, "code", code);
        return postPublic("/auth/2fa/verify/", body);
    }

    public static void forgotPassword(String email) throws ApiException, IOException, InterruptedException {
        postPublic("/auth/forgot-password/", Map.of("email", email));
    }

    public static void resetPassword(String token, String newPassword) throws ApiException, IOException, InterruptedException {
        postPublic("/auth/reset-password/", Map.of("token", token, "new_password", newPassword));
    }

    /** Tente de rafraîchir l'access token via le refresh token stocké en session. */
    public static boolean tryRefreshToken() {
        try {
            String refresh = Session.getRefreshToken();
            if (refresh == null) return false;
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/auth/refresh/"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(Map.of("refresh", refresh))))
                    .build();
            HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode node = MAPPER.readTree(resp.body());
                Session.updateAccessToken(node.get("access").asText());
                return true;
            }
        } catch (Exception ignored) { }
        return false;
    }

    // ---------------------------------------------------------------
    // Requêtes génériques authentifiées (Bearer JWT)
    // ---------------------------------------------------------------

    public static JsonNode get(String path) throws ApiException, IOException, InterruptedException {
        return authedRequest("GET", path, null);
    }

    public static JsonNode post(String path, Object body) throws ApiException, IOException, InterruptedException {
        return authedRequest("POST", path, body);
    }

    public static JsonNode patch(String path, Object body) throws ApiException, IOException, InterruptedException {
        return authedRequest("PATCH", path, body);
    }

    public static void delete(String path) throws ApiException, IOException, InterruptedException {
        authedRequest("DELETE", path, null);
    }

    private static JsonNode authedRequest(String method, String path, Object body) throws ApiException, IOException, InterruptedException {
        JsonNode result = doAuthedRequest(method, path, body);
        return result;
    }

    private static JsonNode doAuthedRequest(String method, String path, Object body) throws ApiException, IOException, InterruptedException {
        HttpResponse<String> resp = sendAuthed(method, path, body);
        if (resp.statusCode() == 401 && tryRefreshToken()) {
            // Le token a expiré (durée de vie 2h) : on retente une fois après refresh.
            resp = sendAuthed(method, path, body);
        }
        return handleResponse(resp);
    }

    private static HttpResponse<String> sendAuthed(String method, String path, Object body) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Authorization", "Bearer " + Session.getAccessToken())
                .header("Content-Type", "application/json");

        HttpRequest.BodyPublisher publisher = body == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body));

        builder.method(method, publisher);
        return HTTP.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static JsonNode postPublic(String path, Object body) throws ApiException, IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)))
                .build();
        HttpResponse<String> resp = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return handleResponse(resp);
    }

    private static JsonNode handleResponse(HttpResponse<String> resp) throws ApiException, IOException {
        String rawBody = resp.body() == null || resp.body().isBlank() ? "{}" : resp.body();
        JsonNode node = MAPPER.readTree(rawBody);
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return node;
        }
        String message = extractErrorMessage(node);
        throw new ApiException(resp.statusCode(), message);
    }

    private static String extractErrorMessage(JsonNode node) {
        if (node.has("detail")) return node.get("detail").asText();
        // DRF renvoie souvent {"champ": ["erreur1", "erreur2"]}
        StringBuilder sb = new StringBuilder();
        node.fields().forEachRemaining(entry -> {
            if (entry.getValue().isArray()) {
                for (JsonNode item : entry.getValue()) {
                    sb.append(entry.getKey()).append(" : ").append(item.asText()).append("\n");
                }
            } else {
                sb.append(entry.getKey()).append(" : ").append(entry.getValue().asText()).append("\n");
            }
        });
        return sb.length() > 0 ? sb.toString().trim() : "Erreur inconnue (" + node + ")";
    }

    public static List<Object> extractResults(JsonNode node) throws IOException {
        JsonNode arr = node.has("results") ? node.get("results") : node;
        return MAPPER.convertValue(arr, List.class);
    }
}
