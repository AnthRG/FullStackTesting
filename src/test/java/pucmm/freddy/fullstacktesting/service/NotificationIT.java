package pucmm.freddy.fullstacktesting.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.domain.Notification;
import pucmm.freddy.fullstacktesting.domain.NotificationRepository;
import pucmm.freddy.fullstacktesting.domain.NotificationType;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.NotificationListResponse;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.dto.StockMovementRequest;
import pucmm.freddy.fullstacktesting.exception.NotificationNotFoundException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationIT extends AbstractIntegrationTest {

    @Autowired
    private NotificationService service;

    @Autowired
    private ProductService productService;

    @Autowired
    private StockMovementService stockMovementService;

    @Autowired
    private NotificationRepository notificationRepository;

    @PersistenceContext
    private EntityManager em;

    private ProductResponse crearProducto(String token, int quantity, int minimumStock) {
        return productService.create(new ProductRequest(
                "Producto " + token, "SKU-" + token + "-" + System.nanoTime(), "desc", "cat",
                new BigDecimal("10.00"), quantity, minimumStock, ProductStatus.ACTIVE));
    }

    private List<Notification> notificacionesDe(Long productId) {
        em.flush();
        return notificationRepository.findAll().stream()
                .filter(n -> n.getProduct().getId().equals(productId))
                .toList();
    }

    // ── disparo desde StockMovementService ───────────────────────────────────

    @Test
    void registrarMovimientoOutQueCruzaElUmbral_persisteLaAlertaNoLeida() {
        ProductResponse producto = crearProducto("cruza", 10, 5);

        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "venta"));

        List<Notification> alertas = notificacionesDe(producto.id());
        assertThat(alertas).hasSize(1);
        Notification alerta = alertas.get(0);
        assertThat(alerta.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(alerta.getQuantity()).isEqualTo(2);
        assertThat(alerta.getMinimumStock()).isEqualTo(5);
        assertThat(alerta.getProductName()).isEqualTo(producto.name());
        assertThat(alerta.getProductSku()).isEqualTo(producto.sku());
        assertThat(alerta.isRead()).isFalse();
        assertThat(alerta.getReadAt()).isNull();
        assertThat(alerta.getCreatedAt()).isNotNull();
        assertThat(alerta.getMessage()).contains("Stock bajo", producto.sku());
    }

    @Test
    void registrarMovimientoOutQueNoCruzaElUmbral_noGeneraAlerta() {
        ProductResponse producto = crearProducto("nocruza", 20, 5);

        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 3, "venta"));

        assertThat(notificacionesDe(producto.id())).isEmpty();
    }

    @Test
    void registrarMovimientoQueDejaElStockEnCero_generaAlertaOutOfStock() {
        ProductResponse producto = crearProducto("cero", 10, 5);

        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 10, "venta"));

        assertThat(notificacionesDe(producto.id()))
                .singleElement()
                .satisfies(n -> assertThat(n.getType()).isEqualTo(NotificationType.OUT_OF_STOCK));
    }

    @Test
    void segundoMovimientoEstandoYaBajoMinimo_noGeneraAlertaRepetida() {
        ProductResponse producto = crearProducto("repetido", 10, 5);

        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "primera"));
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 1, "segunda"));

        assertThat(notificacionesDe(producto.id())).hasSize(1);
    }

    // ── disparo desde ProductService ─────────────────────────────────────────

    @Test
    void crearProductoYaBajoMinimo_generaAlerta() {
        ProductResponse producto = crearProducto("nacebajo", 1, 5);

        assertThat(notificacionesDe(producto.id()))
                .singleElement()
                .satisfies(n -> assertThat(n.getType()).isEqualTo(NotificationType.LOW_STOCK));
    }

    @Test
    void actualizarProductoSubiendoElMinimo_generaAlerta() {
        ProductResponse producto = crearProducto("subeminimo", 10, 2);
        assertThat(notificacionesDe(producto.id())).isEmpty();

        productService.update(producto.id(), new ProductRequest(
                producto.name(), producto.sku(), "desc", "cat",
                new BigDecimal("10.00"), 10, 20, ProductStatus.ACTIVE));

        assertThat(notificacionesDe(producto.id()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.getType()).isEqualTo(NotificationType.LOW_STOCK);
                    assertThat(n.getMinimumStock()).isEqualTo(20);
                });
    }

    // ── lectura y marcado ────────────────────────────────────────────────────

    @Test
    void list_devuelveLaAlertaConSusDatosYElContadorDeNoLeidas() {
        long noLeidasAntes = service.unreadCount();
        ProductResponse producto = crearProducto("listar", 10, 5);
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "venta"));
        em.flush();

        NotificationListResponse listado = service.list(false);

        assertThat(listado.unreadCount()).isEqualTo(noLeidasAntes + 1);
        assertThat(listado.items())
                .filteredOn(n -> n.productId().equals(producto.id()))
                .singleElement()
                .satisfies(n -> {
                    assertThat(n.type()).isEqualTo(NotificationType.LOW_STOCK);
                    assertThat(n.productSku()).isEqualTo(producto.sku());
                    assertThat(n.quantity()).isEqualTo(2);
                    assertThat(n.read()).isFalse();
                });
    }

    @Test
    void markRead_cambiaElEstadoYBajaElContador() {
        ProductResponse producto = crearProducto("marcar", 10, 5);
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "venta"));
        Notification alerta = notificacionesDe(producto.id()).get(0);
        long noLeidasAntes = service.unreadCount();

        service.markRead(alerta.getId());
        em.flush();

        Notification recargada = notificationRepository.findById(alerta.getId()).orElseThrow();
        assertThat(recargada.isRead()).isTrue();
        assertThat(recargada.getReadAt()).isNotNull();
        assertThat(service.unreadCount()).isEqualTo(noLeidasAntes - 1);
    }

    @Test
    void markRead_conIdInexistente_lanzaNotificationNotFoundException() {
        assertThatThrownBy(() -> service.markRead(999_999L))
                .isInstanceOf(NotificationNotFoundException.class);
    }

    @Test
    void markAllRead_dejaElContadorEnCero() {
        ProductResponse producto = crearProducto("marcartodas", 10, 5);
        stockMovementService.register(
                new StockMovementRequest(producto.id(), MovementType.OUT, 8, "venta"));
        em.flush();
        assertThat(service.unreadCount()).isPositive();

        service.markAllRead();

        assertThat(service.unreadCount()).isZero();
        assertThat(service.list(true).items()).isEmpty();
    }
}
