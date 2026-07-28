package pucmm.freddy.fullstacktesting.security;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("security")
class SecurityHeadersIT extends AbstractSecurityTest {

    @ParameterizedTest
    @ValueSource(strings = {"/v3/api-docs", "/actuator/health", "/api/products"})
    void lasRutasSinHtmlUsanLaPoliticaEstricta(String path) {
        String csp = cabecerasDe(path).getFirst("Content-Security-Policy");

        assertThat(csp).as("%s sin Content-Security-Policy", path).isNotBlank();
        assertThat(csp).contains("default-src 'none'").contains("frame-ancestors 'none'");
        assertThat(csp)
                .as("la relajacion para Swagger UI no puede filtrarse al resto de la API")
                .doesNotContain("unsafe-inline");
    }

    @Test
    void swaggerUiRecibeLaPoliticaRelajadaYUnaSola() {
        List<String> csp = cabecerasDe("/swagger-ui/index.html").get("Content-Security-Policy");

        assertThat(csp)
                .as("dos cabeceras CSP se combinan por interseccion y romperian la pagina")
                .hasSize(1);
        assertThat(csp.getFirst())
                .contains("style-src 'self' 'unsafe-inline'")
                .contains("frame-ancestors 'none'");
    }

    @Test
    void laApiTraeElRestoDeCabecerasDeEndurecimiento() {
        HttpHeaders cabeceras = cabecerasDe("/api/products");

        assertThat(cabeceras.getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(cabeceras.getFirst("X-Frame-Options")).isEqualTo("DENY");
        assertThat(cabeceras.getFirst("Cache-Control")).contains("no-store");
    }

    private HttpHeaders cabecerasDe(String path) {
        return client().get().uri(path)
                .exchange((request, response) -> response.getHeaders());
    }
}
