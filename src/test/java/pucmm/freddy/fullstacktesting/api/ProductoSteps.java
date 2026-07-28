package pucmm.freddy.fullstacktesting.api;

import io.cucumber.java.es.Cuando;
import io.cucumber.java.es.Dado;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Steps del CRUD de productos. Cada step deja la respuesta en el {@link ScenarioContext};
 * quien afirma sobre ella es {@link RespuestaSteps}.
 */
public class ProductoSteps {

    private final ApiClient api;
    private final ScenarioContext context;

    public ProductoSteps(ApiClient api, ScenarioContext context) {
        this.api = api;
        this.context = context;
    }

    @Dado("que existe un producto con cantidad {int}")
    public void queExisteUnProductoConCantidad(int cantidad) {
        Map<String, Object> body = productoValido();
        body.put("quantity", cantidad);

        Long id = api.comoAdmin().body(body).post("/api/products")
                .then().statusCode(201)
                .extract().jsonPath().getLong("id");

        context.setProductId(id);
        context.setCantidadInicial(cantidad);
    }

    @Cuando("creo un producto valido")
    public void creoUnProductoValido() {
        context.setResponse(api.autenticada().body(productoValido()).post("/api/products"));
        // Solo se lee el id cuando la creacion fue aceptada. Un 403 de Spring Security llega
        // sin cuerpo, asi que intentar parsearlo como JSON revienta antes del assert real.
        if (context.getResponse().statusCode() == 201) {
            context.setProductId(context.getResponse().jsonPath().getLong("id"));
        }
    }

    @Cuando("creo otro producto con el mismo SKU")
    public void creoOtroProductoConElMismoSku() {
        String sku = api.comoAdmin().get("/api/products/" + context.getProductId())
                .then().statusCode(200)
                .extract().jsonPath().getString("sku");

        Map<String, Object> duplicado = productoValido();
        duplicado.put("sku", sku);

        context.setResponse(api.autenticada().body(duplicado).post("/api/products"));
    }

    @Cuando("creo un producto sin nombre")
    public void creoUnProductoSinNombre() {
        Map<String, Object> body = productoValido();
        body.put("name", "");
        context.setResponse(api.autenticada().body(body).post("/api/products"));
    }

    @Cuando("creo un producto con precio negativo")
    public void creoUnProductoConPrecioNegativo() {
        Map<String, Object> body = productoValido();
        body.put("price", -5.00);
        context.setResponse(api.autenticada().body(body).post("/api/products"));
    }

    @Cuando("consulto el producto creado")
    public void consultoElProductoCreado() {
        context.setResponse(api.autenticada().get("/api/products/" + context.getProductId()));
    }

    @Cuando("consulto el producto con id {long}")
    public void consultoElProductoConId(long id) {
        context.setResponse(api.autenticada().get("/api/products/" + id));
    }

    @Cuando("actualizo el producto creado con el nombre {string}")
    public void actualizoElProductoCreadoConElNombre(String nombre) {
        Map<String, Object> actual = api.comoAdmin().get("/api/products/" + context.getProductId())
                .then().statusCode(200)
                .extract().jsonPath().getMap("$");

        // El PUT reemplaza el recurso completo, asi que se reenvia todo con el nombre cambiado.
        Map<String, Object> body = new HashMap<>();
        body.put("name", nombre);
        body.put("sku", actual.get("sku"));
        body.put("description", actual.get("description"));
        body.put("category", actual.get("category"));
        body.put("price", actual.get("price"));
        body.put("quantity", actual.get("quantity"));
        body.put("minimumStock", actual.get("minimumStock"));
        body.put("status", actual.get("status"));

        context.setResponse(api.autenticada().body(body).put("/api/products/" + context.getProductId()));
    }

    @Cuando("elimino el producto creado")
    public void eliminoElProductoCreado() {
        context.setResponse(api.autenticada().delete("/api/products/" + context.getProductId()));
    }

    @Cuando("listo los productos")
    public void listoLosProductos() {
        context.setResponse(api.autenticada().get("/api/products"));
    }

    /**
     * SKU unico por llamada: la base es la misma para toda la suite y el SKU tiene
     * restriccion de unicidad, asi que un valor fijo haria fallar el segundo escenario.
     */
    private Map<String, Object> productoValido() {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Producto de prueba de contrato");
        body.put("sku", "API-" + UUID.randomUUID().toString().substring(0, 8));
        body.put("description", "Creado por los escenarios de Cucumber");
        body.put("category", "Pruebas");
        body.put("price", 19.99);
        body.put("quantity", 10);
        body.put("minimumStock", 2);
        body.put("status", "ACTIVE");
        return body;
    }
}
