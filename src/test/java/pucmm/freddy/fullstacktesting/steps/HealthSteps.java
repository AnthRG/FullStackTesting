package pucmm.freddy.fullstacktesting.steps;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;
import io.cucumber.java.es.Y;

public class HealthSteps {

    @LocalServerPort
    private int port;

    private ResponseEntity<String> response;

    @Cuando("consulto el endpoint de salud")
    public void consultoElEndpointDeSalud() {
        response = RestClient.create("http://localhost:" + port)
                .get()
                .uri("/actuator/health")
                .retrieve()
                .toEntity(String.class);
    }

    @Entonces("la respuesta tiene codigo {int}")
    public void laRespuestaTieneCodigo(int codigo) {
        assertThat(response.getStatusCode().value()).isEqualTo(codigo);
    }

    @Y("el estado reportado es {string}")
    public void elEstadoReportadoEs(String estado) {
        assertThat(response.getBody()).contains("\"status\":\"" + estado + "\"");
    }
}
