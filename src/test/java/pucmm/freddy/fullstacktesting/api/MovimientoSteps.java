package pucmm.freddy.fullstacktesting.api;

import io.cucumber.java.es.Cuando;

import java.util.HashMap;
import java.util.Map;

/**
 * Steps de movimientos de stock. Cubren por HTTP la logica de negocio de
 * StockMovementService.register(): IN suma, OUT resta y falla si no alcanza,
 * ADJUSTMENT fija la cantidad exacta.
 */
public class MovimientoSteps {

    private final ApiClient api;
    private final ScenarioContext context;

    public MovimientoSteps(ApiClient api, ScenarioContext context) {
        this.api = api;
        this.context = context;
    }

    @Cuando("registro una entrada de {int} unidades")
    public void registroUnaEntradaDe(int cantidad) {
        registrar("IN", cantidad, context.getProductId());
    }

    @Cuando("registro una salida de {int} unidades")
    public void registroUnaSalidaDe(int cantidad) {
        registrar("OUT", cantidad, context.getProductId());
    }

    @Cuando("registro un ajuste a {int} unidades")
    public void registroUnAjusteA(int cantidad) {
        registrar("ADJUSTMENT", cantidad, context.getProductId());
    }

    @Cuando("registro una entrada de {int} unidades sobre el producto {long}")
    public void registroUnaEntradaSobreElProducto(int cantidad, long productId) {
        registrar("IN", cantidad, productId);
    }

    @Cuando("consulto el historial de movimientos")
    public void consultoElHistorialDeMovimientos() {
        context.setResponse(api.autenticada().get("/api/stock-movements"));
    }

    private void registrar(String tipo, int cantidad, Long productId) {
        Map<String, Object> body = new HashMap<>();
        body.put("productId", productId);
        body.put("movementType", tipo);
        body.put("quantity", cantidad);
        body.put("observations", "Movimiento de los escenarios de Cucumber");

        context.setResponse(api.autenticada().body(body).post("/api/stock-movements"));
    }
}
