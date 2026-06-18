package pucmm.freddy.fullstacktesting.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import pucmm.freddy.fullstacktesting.config.KeycloakAdminProperties;
import pucmm.freddy.fullstacktesting.dto.RoleView;
import pucmm.freddy.fullstacktesting.dto.UserRolesView;

/**
 * Cliente del backend hacia la Admin REST API de Keycloak.
 *
 * <p>Usa el {@link RestClient} de Spring (sin librerias extra). Cada operacion
 * obtiene un token de administrador y llama los endpoints
 * {@code /admin/realms/{realm}/...}. Se filtran los roles internos de Keycloak
 * ({@code offline_access}, {@code uma_authorization}, {@code default-roles-*})
 * para que la API exponga solo los roles del negocio.</p>
 */
@Service
public class KeycloakAdminClient {

    private static final Set<String> BUILTIN_ROLES = Set.of("offline_access", "uma_authorization");

    private final RestClient rest;
    private final KeycloakAdminProperties props;

    public KeycloakAdminClient(KeycloakAdminProperties props) {
        this.rest = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
                    throw new ResponseStatusException(response.getStatusCode(),
                            "Keycloak admin API " + request.getMethod() + " " + request.getURI()
                                    + " -> " + response.getStatusCode() + " body=" + body);
                })
                .build();
        this.props = props;
    }

    // ----------------------------------------------------------------- lecturas

    /** Lista todos los usuarios del realm con sus roles de realm. */
    public List<UserRolesView> listUsers() {
        String token = accessToken();
        KcUser[] users = rest.get()
                .uri(adminBase() + "/users?max=200")
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(KcUser[].class);
        return users == null ? List.of()
                : Arrays.stream(users).map(u -> toView(u, token)).toList();
    }

    /** Devuelve un usuario y sus roles de realm. */
    public UserRolesView getUser(String userId) {
        String token = accessToken();
        KcUser user = rest.get()
                .uri(adminBase() + "/users/{id}", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(KcUser.class);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no existe: " + userId);
        }
        return toView(user, token);
    }

    /** Roles de realm asignables (excluye los internos de Keycloak). */
    public List<RoleView> assignableRoles() {
        KcRole[] roles = rest.get()
                .uri(adminBase() + "/roles")
                .header(HttpHeaders.AUTHORIZATION, bearer(accessToken()))
                .retrieve()
                .body(KcRole[].class);
        return roles == null ? List.of()
                : Arrays.stream(roles)
                        .filter(r -> !isBuiltin(r.name()))
                        .map(r -> new RoleView(r.name(), r.description()))
                        .toList();
    }

    // --------------------------------------------------------------- escrituras

    /** Asigna un rol de realm existente a un usuario. */
    public void assignRole(String userId, String roleName) {
        String token = accessToken();
        KcRole role = roleByName(roleName, token);
        rest.post()
                .uri(adminBase() + "/users/{id}/role-mappings/realm", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    /** Quita un rol de realm a un usuario. */
    public void removeRole(String userId, String roleName) {
        String token = accessToken();
        KcRole role = roleByName(roleName, token);
        rest.method(HttpMethod.DELETE)
                .uri(adminBase() + "/users/{id}/role-mappings/realm", userId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    // ------------------------------------------------------------------ helpers

    private UserRolesView toView(KcUser user, String token) {
        KcRole[] roles = rest.get()
                .uri(adminBase() + "/users/{id}/role-mappings/realm", user.id())
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .retrieve()
                .body(KcRole[].class);
        List<String> names = roles == null ? List.of()
                : Arrays.stream(roles).map(KcRole::name).filter(n -> !isBuiltin(n)).toList();
        return new UserRolesView(
                user.id(), user.username(), user.email(),
                user.enabled() != null && user.enabled(), names);
    }

    private KcRole roleByName(String roleName, String token) {
        try {
            return rest.get()
                    .uri(adminBase() + "/roles/{name}", roleName)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .retrieve()
                    .body(KcRole.class);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode().value() == 404) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no existe: " + roleName);
            }
            throw e;
        }
    }

    /** Obtiene un token de administrador (grant password contra el auth-realm). */
    private String accessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", props.clientId());
        form.add("username", props.username());
        form.add("password", props.password());
        TokenResponse token = rest.post()
                .uri(props.serverUrl() + "/realms/{realm}/protocol/openid-connect/token", props.authRealm())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(TokenResponse.class);
        if (token == null || token.accessToken() == null) {
            throw new IllegalStateException("No se pudo obtener token de admin de Keycloak");
        }
        return token.accessToken();
    }

    private String adminBase() {
        return props.serverUrl() + "/admin/realms/" + props.targetRealm();
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }

    private static boolean isBuiltin(String roleName) {
        return BUILTIN_ROLES.contains(roleName) || roleName.startsWith("default-roles-");
    }

    // ----------------------- Representaciones internas de Keycloak 

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KcUser(String id, String username, String email, Boolean enabled) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KcRole(String id, String name, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(@JsonProperty("access_token") String accessToken) {
    }
}
