package pucmm.freddy.fullstacktesting.api;

import io.cucumber.java.es.Entonces;
import io.restassured.response.Response;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Todas las afirmaciones sobre la ultima respuesta viven aqui, no repartidas por cada
 * step de accion. Eso es lo que permite escribir escenarios nuevos sin escribir Java:
 * "Entonces la respuesta tiene codigo 409" sirve igual para productos que para movimientos.
 */
public class RespuestaSteps {

    private final ScenarioContext context;

    public RespuestaSteps(ScenarioContext context) {
        this.context = context;
    }

    @Entonces("la respuesta tiene codigo {int}")
    public void laRespuestaTieneCodigo(int esperado) {
        assertThat(respuesta().statusCode())
                .as("cuerpo de la respuesta: %s", respuesta().asString())
                .isEqualTo(esperado);
    }

    @Entonces("la respuesta incluye el campo {string}")
    public void laRespuestaIncluyeElCampo(String campo) {
        // El get de JsonPath es generico: sin la variable Object, el compilador lo infiere
        // como Predicate y assertThat queda ambiguo.
        Object valor = respuesta().jsonPath().get(campo);
        assertThat(valor)
                .as("el campo '%s' falta en %s", campo, respuesta().asString())
                .isNotNull();
    }

    @Entonces("el campo {string} vale {int}")
    public void elCampoValeEntero(String campo, int esperado) {
        assertThat(respuesta().jsonPath().getInt(campo)).isEqualTo(esperado);
    }

    @Entonces("el campo {string} vale {string}")
    public void elCampoValeTexto(String campo, String esperado) {
        assertThat(respuesta().jsonPath().getString(campo)).isEqualTo(esperado);
    }

    @Entonces("el error identifica el campo invalido {string}")
    public void elErrorIdentificaElCampoInvalido(String campo) {
        // GlobalExceptionHandler mete los errores de validacion en una propiedad
        // extra del ProblemDetail, con el nombre del campo como clave.
        assertThat(respuesta().jsonPath().getString("errors." + campo))
                .as("no se reporto el campo '%s' en %s", campo, respuesta().asString())
                .isNotBlank();
    }

    @Entonces("la respuesta es un problema con formato RFC 7807")
    public void laRespuestaEsUnProblemaRfc7807() {
        assertThat(respuesta().getContentType()).contains("application/problem+json");
        assertThat(respuesta().jsonPath().getInt("status")).isEqualTo(respuesta().statusCode());
        assertThat(respuesta().jsonPath().getString("title")).isNotBlank();
        assertThat(respuesta().jsonPath().getString("detail")).isNotBlank();
    }

    @Entonces("la lista viene paginada")
    public void laListaVienePaginada() {
        assertThat(respuesta().jsonPath().getList("content")).isNotNull();
        assertThat(respuesta().jsonPath().getInt("totalElements")).isGreaterThanOrEqualTo(0);
        Object totalPages = respuesta().jsonPath().get("totalPages");
        assertThat(totalPages).isNotNull();
    }

    private Response respuesta() {
        assertThat(context.getResponse()).as("ningun step ejecuto una peticion").isNotNull();
        return context.getResponse();
    }
}
