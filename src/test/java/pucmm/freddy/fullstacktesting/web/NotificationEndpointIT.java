package pucmm.freddy.fullstacktesting.web;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.domain.NotificationRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.dto.StockMovementRequest;
import pucmm.freddy.fullstacktesting.service.ProductService;
import pucmm.freddy.fullstacktesting.service.StockMovementService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class NotificationEndpointIT extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private NotificationRepository notificationRepository;

    @BeforeEach
    void limpiarNotificaciones() {
        notificationRepository.deleteAll();
    }

    /** Crea un producto y lo baja del mínimo para dejar exactamente una alerta pendiente. */
    private ProductResponse alertaPendiente(String token) {
        ProductResponse producto = productService.create(new ProductRequest(
                "Producto " + token, "SKU-" + token + "-" + System.nanoTime(), "desc", "cat",
                new BigDecimal("10.00"), 10, 5, ProductStatus.ACTIVE));
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "venta"));
        return producto;
    }

    private RestClient client() {
        return RestClient.create("http://localhost:" + port);
    }

    private Map<?, ?> listar(String token, String query) {
        return client().get().uri("/api/notifications" + query)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(Map.class);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> items(Map<?, ?> body) {
        return (List<Map<String, Object>>) body.get("items");
    }

    // ── seguridad ────────────────────────────────────────────────────────────

    @Test
    void listar_sinToken_devuelve401() {
        int status = client().get().uri("/api/notifications")
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(401);
    }

    @Test
    void listar_conUsuarioSinProductView_devuelve403() {
        int status = client().get().uri("/api/notifications")
                .header("Authorization", "Bearer " + tokenFor("user2", "user2"))
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(403);
    }

    // ── GET /api/notifications ───────────────────────────────────────────────

    @Test
    void listar_conProductView_devuelveItemsYUnreadCount() {
        ProductResponse producto = alertaPendiente("endpoint");

        Map<?, ?> body = listar(tokenFor("user1", "user1"), "");

        assertThat(body.get("unreadCount")).isEqualTo(1);
        assertThat(items(body)).singleElement().satisfies(item -> {
            assertThat(item.get("id")).isNotNull();
            assertThat(item.get("type")).isEqualTo("LOW_STOCK");
            assertThat(item.get("productId")).isEqualTo(producto.id().intValue());
            assertThat(item.get("productName")).isEqualTo(producto.name());
            assertThat(item.get("productSku")).isEqualTo(producto.sku());
            assertThat(item.get("quantity")).isEqualTo(2);
            assertThat(item.get("minimumStock")).isEqualTo(5);
            assertThat(item.get("message")).asString().contains("Stock bajo");
            // El frontend parsea createdAt como LocalDateTime ISO (2026-07-26T13:31:25...).
            assertThat(item.get("createdAt")).asString()
                    .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?");
            assertThat(item.get("read")).isEqualTo(false);
        });
    }

    @Test
    void listar_conOnlyUnread_ocultaLasYaLeidas() {
        alertaPendiente("filtro-a");
        alertaPendiente("filtro-b");
        String token = tokenFor("user1", "user1");
        Object primeraId = items(listar(token, "")).get(0).get("id");

        marcarLeida(token, ((Number) primeraId).longValue());

        assertThat(items(listar(token, "?onlyUnread=true"))).hasSize(1);
        assertThat(items(listar(token, "?onlyUnread=false"))).hasSize(2);
        assertThat(listar(token, "?onlyUnread=true").get("unreadCount")).isEqualTo(1);
    }

    // ── POST /api/notifications/{id}/read ────────────────────────────────────

    @Test
    void marcarLeida_devuelve204YCambiaElEstado() {
        alertaPendiente("leer");
        String token = tokenFor("user1", "user1");
        long id = ((Number) items(listar(token, "")).get(0).get("id")).longValue();

        assertThat(marcarLeida(token, id)).isEqualTo(204);

        Map<?, ?> body = listar(token, "");
        assertThat(body.get("unreadCount")).isEqualTo(0);
        assertThat(items(body).get(0).get("read")).isEqualTo(true);
    }

    @Test
    void marcarLeida_conIdInexistente_devuelve404() {
        int status = client().post().uri("/api/notifications/{id}/read", 999_999L)
                .header("Authorization", "Bearer " + tokenFor("user1", "user1"))
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(404);
    }

    // ── POST /api/notifications/read-all ─────────────────────────────────────

    @Test
    void marcarTodasLeidas_devuelve204YDejaElContadorEnCero() {
        alertaPendiente("todas-a");
        alertaPendiente("todas-b");
        String token = tokenFor("user1", "user1");

        int status = client().post().uri("/api/notifications/read-all")
                .header("Authorization", "Bearer " + token)
                .exchange((req, res) -> res.getStatusCode().value());

        assertThat(status).isEqualTo(204);
        Map<?, ?> body = listar(token, "");
        assertThat(body.get("unreadCount")).isEqualTo(0);
        assertThat(items(body)).allSatisfy(item -> assertThat(item.get("read")).isEqualTo(true));
    }

    private int marcarLeida(String token, long id) {
        return client().post().uri("/api/notifications/{id}/read", id)
                .header("Authorization", "Bearer " + token)
                .exchange((req, res) -> res.getStatusCode().value());
    }
}
