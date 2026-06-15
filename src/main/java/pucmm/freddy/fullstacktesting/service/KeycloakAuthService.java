package pucmm.freddy.fullstacktesting.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import pucmm.freddy.fullstacktesting.dto.LoginResponse;

/**
 * Intercambia usuario/contrasena por un token de Keycloak usando el grant
 * {@code password} (Direct Access Grant) del cliente publico {@code frontend}.
 *
 * <p>El backend llama a Keycloak server-to-server; como Keycloak emite el token
 * con el issuer publico (KC_HOSTNAME = http://localhost:8081), ese mismo token
 * sirve para autenticarse contra el Resource Server.</p>
 */
@Service
public class KeycloakAuthService {

    private final RestClient rest;
    private final String tokenUri;
    private final String clientId;

    public KeycloakAuthService(
            @Value("${keycloak.login.token-uri}") String tokenUri,
            @Value("${keycloak.login.client-id}") String clientId) {
        this.rest = RestClient.builder()
                .requestFactory(new JdkClientHttpRequestFactory())
                .build();
        this.tokenUri = tokenUri;
        this.clientId = clientId;
    }

    /**
     * Autentica al usuario contra Keycloak y devuelve su token.
     *
     * @throws ResponseStatusException 401 si las credenciales son invalidas
     */
    public LoginResponse login(String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", username);
        form.add("password", password);
        try {
            TokenResponse token = rest.post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TokenResponse.class);
            if (token == null || token.accessToken() == null) {
                throw new IllegalStateException("Keycloak no devolvio un token");
            }
            return new LoginResponse(token.accessToken(), token.tokenType(),
                    token.expiresIn(), token.refreshToken());
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 401) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales invalidas");
            }
            throw e;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn,
            @JsonProperty("refresh_token") String refreshToken) {
    }
}
