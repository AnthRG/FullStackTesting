package pucmm.freddy.fullstacktesting.api;

import io.cucumber.java.es.Cuando;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Un unico step generico que sustituye un campo del payload valido por el valor bajo
 * prueba. Es lo que permite escribir toda la matriz de particiones de equivalencia y
 * valores limite en la tabla de ejemplos del feature, sin escribir un step por caso.
 */
public class DatosSteps {

    // Corchetes y no angulos: Gherkin reserva <...> para los placeholders del esquema
    // de escenario y sustituirlos ahi seria una fuente de confusion.
    private static final String VACIO = "[vacio]";
    private static final String NULO = "[nulo]";
    private static final String PREFIJO_TEXTO = "[texto:";

    private final ApiClient api;
    private final ScenarioContext context;

    public DatosSteps(ApiClient api, ScenarioContext context) {
        this.api = api;
        this.context = context;
    }

    @Cuando("creo un producto con {string} igual a {string}")
    public void creoUnProductoConElCampo(String campo, String valor) {
        Map<String, Object> body = Payloads.productoValido();
        body.put(campo, valorDeCampo(campo, valor));

        context.setResponse(api.autenticada().body(body).post("/api/products"));
        // Solo hay id cuando la creacion fue aceptada; los escenarios de rechazo no
        // deben intentar parsear un cuerpo de error como si fuera el recurso.
        if (context.getResponse().statusCode() == 201) {
            context.setProductId(context.getResponse().jsonPath().getLong("id"));
        }
    }

    @Cuando("consulto los movimientos por tipo desde {string} hasta {string}")
    public void consultoLosMovimientosPorTipoEntre(String desde, String hasta) {
        context.setResponse(api.autenticada()
                .queryParam("from", desde)
                .queryParam("to", hasta)
                .get("/api/reports/movements-by-type"));
    }

    /**
     * Convierte el texto de la tabla al tipo que espera el JSON. Sin esto, un precio
     * viajaria como cadena y el 400 vendria de que no se puede deserializar, no de la
     * regla de negocio que se quiere probar.
     */
    private Object valorDeCampo(String campo, String valor) {
        Object literal = literal(valor);
        if (!(literal instanceof String texto)) {
            return literal;
        }
        return switch (campo) {
            case "price" -> new BigDecimal(texto);
            case "quantity", "minimumStock" -> Integer.valueOf(texto);
            default -> texto;
        };
    }

    private Object literal(String valor) {
        if (NULO.equals(valor)) {
            return null;
        }
        if (VACIO.equals(valor)) {
            return "";
        }
        if (valor.startsWith(PREFIJO_TEXTO) && valor.endsWith("]")) {
            int largo = Integer.parseInt(valor.substring(PREFIJO_TEXTO.length(), valor.length() - 1));
            return "A".repeat(largo);
        }
        return valor;
    }
}
