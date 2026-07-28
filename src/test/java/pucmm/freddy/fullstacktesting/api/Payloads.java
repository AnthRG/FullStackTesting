package pucmm.freddy.fullstacktesting.api;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Cuerpos validos de referencia. Los escenarios de datos invalidos parten de uno de estos
 * y cambian un solo campo, de forma que el unico motivo posible de rechazo sea ese campo.
 */
final class Payloads {

    private Payloads() {
    }

    /**
     * SKU unico por llamada: la base es la misma para toda la suite y el SKU tiene
     * restriccion de unicidad, asi que un valor fijo haria fallar el segundo escenario.
     */
    static Map<String, Object> productoValido() {
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
