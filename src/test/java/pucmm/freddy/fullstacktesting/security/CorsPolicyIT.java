package pucmm.freddy.fullstacktesting.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
class CorsPolicyIT extends AbstractSecurityTest {

    private static final String PROTEGIDO = "/api/products";
    private static final String ORIGEN_PERMITIDO = "http://localhost:5173";

    private record Respuesta(int status, HttpHeaders cabeceras) {

        String allowOrigin() {
            return cabeceras.getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://localhost:5173", "http://localhost:3000"})
    void preflightDesdeOrigenConfigurado_esAceptado(String origen) {
        Respuesta respuesta = preflight(origen, "POST", PROTEGIDO);

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.allowOrigin()).isEqualTo(origen);
        assertThat(respuesta.cabeceras().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("POST");
    }

    @Test
    void preflightNoExigeAutenticacion() {
        Respuesta respuesta = preflight(ORIGEN_PERMITIDO, "GET", PROTEGIDO);

        assertThat(respuesta.status())
                .as("si el preflight pidiera token, ningun navegador podria llamar a la API")
                .isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://evil.com",
            "http://localhost:5173.evil.com",
            "https://localhost:5173",
            "http://localhost:5174",
            "null"
    })
    void preflightDesdeOrigenNoConfigurado_esRechazado(String origen) {
        Respuesta respuesta = preflight(origen, "POST", PROTEGIDO);

        assertThat(respuesta.status()).isEqualTo(403);
        assertThat(respuesta.allowOrigin()).isNull();
    }

    @Test
    void preflightConMetodoFueraDeLaLista_esRechazado() {
        Respuesta respuesta = preflight(ORIGEN_PERMITIDO, "TRACE", PROTEGIDO);

        assertThat(respuesta.status()).isEqualTo(403);
        assertThat(respuesta.allowOrigin()).isNull();
    }

    @Test
    void peticionRealDesdeOrigenAjeno_noRecibeCabecerasCors() {
        Respuesta respuesta = peticionReal("http://evil.com", tokenFor("admin", "admin"));

        assertThat(respuesta.status()).isEqualTo(403);
        assertThat(respuesta.allowOrigin())
                .as("sin Access-Control-Allow-Origin el navegador descarta la respuesta")
                .isNull();
    }

    @Test
    void respuestaPermitida_noHabilitaCredenciales() {
        Respuesta respuesta = peticionReal(ORIGEN_PERMITIDO, tokenFor("admin", "admin"));

        assertThat(respuesta.status()).isEqualTo(200);
        assertThat(respuesta.allowOrigin()).isEqualTo(ORIGEN_PERMITIDO);
        assertThat(respuesta.cabeceras().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .as("allowedHeaders='*' con credentials=true seria una combinacion prohibida por el spec")
                .isNull();
    }

    private Respuesta preflight(String origen, String metodoSolicitado, String path) {
        return client().method(HttpMethod.OPTIONS).uri(path)
                .header(HttpHeaders.ORIGIN, origen)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, metodoSolicitado)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type")
                .exchange((request, response) ->
                        new Respuesta(response.getStatusCode().value(), response.getHeaders()));
    }

    private Respuesta peticionReal(String origen, String token) {
        return client().get().uri(PROTEGIDO)
                .header(HttpHeaders.ORIGIN, origen)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange((request, response) ->
                        new Respuesta(response.getStatusCode().value(), response.getHeaders()));
    }
}
