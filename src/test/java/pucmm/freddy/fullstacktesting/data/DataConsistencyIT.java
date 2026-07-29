package pucmm.freddy.fullstacktesting.data;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.InventorySummaryResponse;
import pucmm.freddy.fullstacktesting.dto.ProductRequest;
import pucmm.freddy.fullstacktesting.dto.ProductResponse;
import pucmm.freddy.fullstacktesting.dto.StockMovementRequest;
import pucmm.freddy.fullstacktesting.exception.InsufficientStockException;
import pucmm.freddy.fullstacktesting.service.ProductService;
import pucmm.freddy.fullstacktesting.service.ReportService;
import pucmm.freddy.fullstacktesting.service.StockMovementService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifica los invariantes de los datos que ninguna restriccion de la base puede expresar:
 * que el stock del producto siga siendo la suma de su historial de movimientos y que una
 * operacion rechazada no deje rastro.
 *
 * Sin {@code @Transactional}: hay que dejar que las transacciones se confirmen de verdad
 * para poder comprobar que un rollback ocurrio realmente en la base.
 */
@SpringBootTest
@ActiveProfiles("test")
class DataConsistencyIT extends AbstractIntegrationTest {

    private static final String SKU_PREFIX = "CONSIST-";

    @Autowired
    private ProductService productService;

    @Autowired
    private StockMovementService movementService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void limpiarDatosDePrueba() {
        jdbc.update("DELETE FROM stock_movements WHERE product_id IN "
                + "(SELECT id FROM products WHERE sku LIKE ?)", SKU_PREFIX + "%");
        jdbc.update("DELETE FROM products WHERE sku LIKE ?", SKU_PREFIX + "%");
    }

    @Test
    void elStockReflejaLaSumaAlgebraicaDeLosMovimientos() {
        Long id = crearProducto(10);

        registrar(id, MovementType.IN, 5);
        registrar(id, MovementType.OUT, 3);
        registrar(id, MovementType.IN, 8);

        assertThat(cantidadEnBase(id)).isEqualTo(20);
    }

    @Test
    void unAjusteFijaLaCantidadExactaSinImportarElHistorial() {
        Long id = crearProducto(10);

        registrar(id, MovementType.IN, 40);
        registrar(id, MovementType.ADJUSTMENT, 7);

        assertThat(cantidadEnBase(id)).isEqualTo(7);
    }

    @Test
    void losMovimientosEncadenanLaCantidadAnteriorConLaNueva() {
        Long id = crearProducto(10);

        registrar(id, MovementType.IN, 5);
        registrar(id, MovementType.OUT, 2);
        registrar(id, MovementType.ADJUSTMENT, 30);

        List<Map<String, Object>> historial = jdbc.queryForList(
                "SELECT previous_quantity, new_quantity FROM stock_movements "
                        + "WHERE product_id = ? ORDER BY id", id);

        assertThat(historial).hasSize(3);
        assertThat(historial.get(0).get("previous_quantity")).isEqualTo(10);

        for (int i = 0; i < historial.size() - 1; i++) {
            assertThat(historial.get(i + 1).get("previous_quantity"))
                    .as("el movimiento %d arranca donde termino el anterior", i + 2)
                    .isEqualTo(historial.get(i).get("new_quantity"));
        }

        assertThat(historial.get(historial.size() - 1).get("new_quantity"))
                .isEqualTo(cantidadEnBase(id));
    }

    @Test
    void unaSalidaMayorAlStock_seRechazaSinDejarRastroEnLaBase() {
        Long id = crearProducto(5);

        assertThatThrownBy(() -> registrar(id, MovementType.OUT, 6))
                .isInstanceOf(InsufficientStockException.class);

        assertThat(cantidadEnBase(id)).as("el stock no se toco").isEqualTo(5);
        assertThat(contarMovimientos(id)).as("no quedo un movimiento huerfano").isZero();
    }

    @Test
    void elResumenDeInventarioCuadraConLoQueHayEnLasTablas() {
        Long id = crearProducto(12);
        registrar(id, MovementType.IN, 3);

        InventorySummaryResponse resumen = reportService.summary();

        Long productosEnBase = jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        Long activosEnBase = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE status = 'ACTIVE'", Long.class);
        Long unidadesEnBase = jdbc.queryForObject(
                "SELECT COALESCE(SUM(quantity), 0) FROM products", Long.class);
        Long movimientosEnBase = jdbc.queryForObject("SELECT COUNT(*) FROM stock_movements", Long.class);

        assertThat(resumen.totalProducts()).isEqualTo(productosEnBase);
        assertThat(resumen.activeProducts()).isEqualTo(activosEnBase);
        assertThat(resumen.totalUnits()).isEqualTo(unidadesEnBase);
        assertThat(resumen.totalMovements()).isEqualTo(movimientosEnBase);
    }

    @Test
    void elValorDelInventarioCoincideConPrecioPorCantidad() {
        crearProducto(4);

        InventorySummaryResponse resumen = reportService.summary();

        BigDecimal valorEnBase = jdbc.queryForObject(
                "SELECT COALESCE(SUM(price * quantity), 0) FROM products", BigDecimal.class);

        assertThat(resumen.inventoryValue()).isEqualByComparingTo(valorEnBase);
    }

    private Long crearProducto(int cantidadInicial) {
        ProductResponse creado = productService.create(new ProductRequest(
                "Producto de consistencia",
                SKU_PREFIX + System.nanoTime(),
                "creado por el test de consistencia",
                "datos",
                new BigDecimal("25.50"),
                cantidadInicial,
                2,
                ProductStatus.ACTIVE));
        return creado.id();
    }

    private void registrar(Long productId, MovementType tipo, int cantidad) {
        movementService.register(new StockMovementRequest(productId, tipo, cantidad, "test de datos"));
    }

    private Integer cantidadEnBase(Long productId) {
        return jdbc.queryForObject("SELECT quantity FROM products WHERE id = ?", Integer.class, productId);
    }

    private Long contarMovimientos(Long productId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM stock_movements WHERE product_id = ?", Long.class, productId);
    }
}
