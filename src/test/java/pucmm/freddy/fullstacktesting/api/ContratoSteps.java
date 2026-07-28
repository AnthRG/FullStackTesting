package pucmm.freddy.fullstacktesting.api;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Entonces;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Steps del contrato OpenAPI.
 *
 * <p>El contrato que publica la API en /v3/api-docs es lo que lee quien la consume para
 * programar contra ella. Estos escenarios verifican que ese documento describa lo que la
 * aplicacion realmente expone; un contrato que miente rompe al cliente sin que el servidor
 * de ningun error.
 */
public class ContratoSteps {

    private final ApiClient api;
    private final ScenarioContext context;

    public ContratoSteps(ApiClient api, ScenarioContext context) {
        this.api = api;
        this.context = context;
    }

    @Cuando("consulto el contrato OpenAPI")
    public void consultoElContratoOpenApi() {
        // /v3/api-docs/** esta en permitAll: la documentacion es publica a proposito.
        context.setResponse(api.anonima().get("/v3/api-docs"));
    }

    @Entonces("el documento declara una version de OpenAPI 3")
    public void elDocumentoDeclaraUnaVersionDeOpenApi3() {
        assertThat(context.getResponse().jsonPath().getString("openapi")).startsWith("3.");
    }

    @Entonces("el contrato declara la ruta {string}")
    public void elContratoDeclaraLaRuta(String ruta) {
        // Las claves de "paths" traen barras y puntos, asi que no se pueden navegar con
        // jsonPath por notacion de punto: se saca el mapa y se busca la clave.
        Map<String, ?> paths = context.getResponse().jsonPath().getMap("paths");
        assertThat(paths).as("rutas publicadas en el contrato").containsKey(ruta);
    }

    @Entonces("la ruta {string} declara la operacion {string}")
    public void laRutaDeclaraLaOperacion(String ruta, String metodo) {
        Map<String, ?> paths = context.getResponse().jsonPath().getMap("paths");
        assertThat(paths).containsKey(ruta);

        @SuppressWarnings("unchecked")
        Map<String, ?> operaciones = (Map<String, ?>) paths.get(ruta);
        assertThat(operaciones).as("operaciones de %s", ruta).containsKey(metodo);
    }

    @Entonces("el contrato declara el esquema de seguridad Bearer JWT")
    public void elContratoDeclaraElEsquemaDeSeguridadBearerJwt() {
        var jsonPath = context.getResponse().jsonPath();
        assertThat(jsonPath.getString("components.securitySchemes.bearer-jwt.type"))
                .isEqualTo("http");
        assertThat(jsonPath.getString("components.securitySchemes.bearer-jwt.scheme"))
                .isEqualTo("bearer");
        assertThat(jsonPath.getString("components.securitySchemes.bearer-jwt.bearerFormat"))
                .isEqualTo("JWT");
    }
}
