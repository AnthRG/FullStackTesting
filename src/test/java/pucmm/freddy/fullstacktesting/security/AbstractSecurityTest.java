package pucmm.freddy.fullstacktesting.security;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;

import java.util.Map;

@Tag("security")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
abstract class AbstractSecurityTest extends AbstractIntegrationTest {

    protected static final String REALM = "fullstacktesting";
    protected static final String FRONTEND_CLIENT = "frontend";

    @LocalServerPort
    protected int port;

    protected String baseUrl() {
        return "http://localhost:" + port;
    }

    protected RestClient client() {
        return RestClient.create(baseUrl());
    }

    protected int statusOf(HttpMethod method, String path, String bearerToken) {
        return statusOf(method, path, bearerToken, null);
    }

    protected int statusOf(HttpMethod method, String path, String bearerToken, String cuerpoJson) {
        return statusWithHeader(method, path, bearerToken == null ? null : "Bearer " + bearerToken, cuerpoJson);
    }

    protected int statusWithHeader(HttpMethod method, String path, String authorizationHeader) {
        return statusWithHeader(method, path, authorizationHeader, null);
    }

    protected int statusWithHeader(HttpMethod method, String path, String authorizationHeader, String cuerpoJson) {
        RestClient.RequestBodySpec spec = client().method(method).uri(path);
        if (authorizationHeader != null) {
            spec.header("Authorization", authorizationHeader);
        }
        if (cuerpoJson != null) {
            spec.contentType(MediaType.APPLICATION_JSON).body(cuerpoJson);
        }
        return spec.exchange((request, response) -> response.getStatusCode().value());
    }

    protected String wwwAuthenticateOf(String path, String bearerToken) {
        return client().get().uri(path)
                .header("Authorization", "Bearer " + bearerToken)
                .exchange((request, response) -> response.getHeaders().getFirst("WWW-Authenticate"));
    }

    protected static String realmUrl(String realm) {
        return authServerUrl() + "/realms/" + realm;
    }

    protected static String tokenEndpoint(String realm) {
        return realmUrl(realm) + "/protocol/openid-connect/token";
    }

    protected static String jwkSetUri() {
        return realmUrl(REALM) + "/protocol/openid-connect/certs";
    }

    protected static TokenResponse requestToken(String realm, String clientId, String username, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("username", username);
        form.add("password", password);
        return RestClient.create().post()
                .uri(tokenEndpoint(realm))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .exchange((request, response) -> new TokenResponse(
                        response.getStatusCode().value(),
                        response.bodyTo(new ParameterizedTypeReference<Map<String, Object>>() {})));
    }

    protected record TokenResponse(int status, Map<String, Object> body) {

        String accessToken() {
            return value("access_token");
        }

        String error() {
            return value("error");
        }

        String errorDescription() {
            return value("error_description");
        }

        private String value(String key) {
            return body == null ? null : (String) body.get(key);
        }
    }
}
