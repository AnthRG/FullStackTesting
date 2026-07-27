package pucmm.freddy.fullstacktesting.web;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifica la matriz de permisos endpoint por endpoint, con tokens reales de Keycloak.
 *
 * <p>Cada caso responde una pregunta concreta: quien SI puede hacer algo, y quien NO.
 * Los 403 importan tanto como los 200: sin ellos no hay evidencia de que los permisos
 * separen nada.
 *
 * <p>Detalle a tener presente al leer los tests: Spring MVC resuelve y valida el
 * {@code @RequestBody} ANTES de invocar el metodo del controller, que es donde actua
 * {@code @PreAuthorize}. Un body invalido daria 400 en vez de 403, asi que todos los
 * cuerpos que se envian aqui son validos a proposito.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthorizationIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("sin token, listar productos responde 401")
    void sinToken_productos_401() {
        assertThat(get("/api/products", null)).isEqualTo(401);
    }

    @Test
    @DisplayName("sin token, la administracion de usuarios responde 401")
    void sinToken_admin_401() {
        //cerramos: /api/admin/** estaba en permitAll(),
        // asi que la gestion de usuarios y roles de Keycloak era publica.
        assertThat(get("/api/admin/users", null)).isEqualTo(401);
    }

    // ---------- user2: autenticado pero sin ningun permiso ----------

    @Test
    @DisplayName("user2 no tiene ningun permiso, asi que no ve productos")
    void user2SinPermisos_productos_403() {
        assertThat(get("/api/products", tokenFor("user2", "user2"))).isEqualTo(403);
    }

    // ---------- user1: INVENTORY_VIEWER, solo lectura ----------

    @Test
    @DisplayName("user1 tiene product:view y lista productos")
    void user1ConProductView_listaProductos_200() {
        assertThat(get("/api/products", tokenFor("user1", "user1"))).isEqualTo(200);
    }

    @Test
    @DisplayName("user1 no tiene product:manage, asi que no puede crear productos")
    void user1SinProductManage_creaProducto_403() {
        assertThat(post("/api/products", nuevoProducto(), tokenFor("user1", "user1"))).isEqualTo(403);
    }

    @Test
    @DisplayName("user1 hereda stock:view de INVENTORY_VIEWER y ve los movimientos")
    void user1ConStockView_listaMovimientos_200() {
        assertThat(get("/api/stock-movements", tokenFor("user1", "user1"))).isEqualTo(200);
    }

    @Test
    @DisplayName("user1 no tiene stock:manage, asi que no puede registrar movimientos")
    void user1SinStockManage_registraMovimiento_403() {
        Long productId = crearProductoComoAdmin();

        assertThat(post("/api/stock-movements", entradaDeStock(productId), tokenFor("user1", "user1")))
                .isEqualTo(403);
    }

    @Test
    @DisplayName("user1 hereda report:view y consulta el resumen")
    void user1ConReportView_reportes_200() {
        assertThat(get("/api/reports/summary", tokenFor("user1", "user1"))).isEqualTo(200);
    }

    @Test
    @DisplayName("user1 no tiene audit:view, asi que no consulta la auditoria")
    void user1SinAuditView_auditoria_403() {
        assertThat(get("/api/audit/products", tokenFor("user1", "user1"))).isEqualTo(403);
    }

    // ---------- operator: INVENTORY_OPERATOR, opera pero no audita ni administra ----------

    @Test
    @DisplayName("operator tiene stock:manage y registra un movimiento")
    void operatorConStockManage_registraMovimiento_201() {
        Long productId = crearProductoComoAdmin();

        assertThat(post("/api/stock-movements", entradaDeStock(productId), tokenFor("operator", "operator")))
                .isEqualTo(201);
    }

    @Test
    @DisplayName("operator no tiene audit:view, asi que no consulta la auditoria")
    void operatorSinAuditView_auditoria_403() {
        assertThat(get("/api/audit/products", tokenFor("operator", "operator"))).isEqualTo(403);
    }

    @Test
    @DisplayName("operator no tiene user:manage, asi que no administra usuarios")
    void operatorSinUserManage_admin_403() {
        assertThat(get("/api/admin/users", tokenFor("operator", "operator"))).isEqualTo(403);
    }

    // ---------- auditor: AUDITOR, lee auditoria pero no modifica nada ----------

    @Test
    @DisplayName("auditor tiene audit:view y consulta la auditoria")
    void auditorConAuditView_auditoria_200() {
        assertThat(get("/api/audit/products", tokenFor("auditor", "auditor"))).isEqualTo(200);
    }

    @Test
    @DisplayName("auditor no tiene product:manage, asi que no puede crear productos")
    void auditorSinProductManage_creaProducto_403() {
        assertThat(post("/api/products", nuevoProducto(), tokenFor("auditor", "auditor"))).isEqualTo(403);
    }

    // ---------- admin: INVENTORY_ADMIN, todo ----------

    @Test
    @DisplayName("admin hereda user:manage de INVENTORY_ADMIN y administra usuarios")
    void adminConUserManage_admin_200() {
        assertThat(get("/api/admin/users", tokenFor("admin", "admin"))).isEqualTo(200);
    }

    // ---------- helpers ----------

    /**
     * {@code exchange} deja la respuesta en manos de la funcion que se le pasa, sin aplicar
     * los manejadores de error por defecto, asi que un 401 o un 403 llegan como codigo y no
     * como excepcion.
     */
    private int get(String path, String token) {
        return client().get().uri(path)
                .headers(h -> { if (token != null) h.setBearerAuth(token); })
                .exchange((req, res) -> res.getStatusCode().value());
    }

    private int post(String path, Object body, String token) {
        return client().post().uri(path)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .headers(h -> { if (token != null) h.setBearerAuth(token); })
                .body(body)
                .exchange((req, res) -> res.getStatusCode().value());
    }

    private Long crearProductoComoAdmin() {
        Map<?, ?> creado = client().post().uri("/api/products")
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .headers(h -> h.setBearerAuth(tokenFor("admin", "admin")))
                .body(nuevoProducto())
                .retrieve()
                .body(Map.class);
        return ((Number) creado.get("id")).longValue();
    }

    private Map<String, Object> nuevoProducto() {
        return Map.of(
                "name", "Producto de prueba de permisos",
                "sku", "PERM-" + UUID.randomUUID().toString().substring(0, 8),
                "description", "Creado por AuthorizationIT",
                "category", "Pruebas",
                "price", new BigDecimal("19.99"),
                "quantity", 10,
                "minimumStock", 2,
                "status", "ACTIVE");
    }

    private Map<String, Object> entradaDeStock(Long productId) {
        return Map.of(
                "productId", productId,
                "movementType", "IN",
                "quantity", 5,
                "observations", "Entrada de prueba de permisos");
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }
}
