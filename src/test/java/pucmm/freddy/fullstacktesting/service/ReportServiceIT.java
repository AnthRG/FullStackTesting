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
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.domain.StockMovement;
import pucmm.freddy.fullstacktesting.domain.StockMovementRepository;
import pucmm.freddy.fullstacktesting.dto.InventorySummaryResponse;
import pucmm.freddy.fullstacktesting.dto.LowStockProductResponse;
import pucmm.freddy.fullstacktesting.dto.MovementsByTypeResponse;
import pucmm.freddy.fullstacktesting.dto.TopProductResponse;
import pucmm.freddy.fullstacktesting.exception.InvalidDateRangeException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ReportServiceIT extends AbstractIntegrationTest {

    @Autowired
    private ReportService service;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @PersistenceContext
    private EntityManager em;

    @Test
    void summary_agregaProductosUnidadesValorYCriticos() {
        InventorySummaryResponse before = service.summary();

        Product a1 = saveProduct("summary-a1", ProductStatus.ACTIVE, "10.00", 5, 1);
        saveProduct("summary-a2", ProductStatus.ACTIVE, "20.00", 10, 3);
        saveProduct("summary-in", ProductStatus.INACTIVE, "5.00", 2, 8); // quantity <= minimumStock -> critico
        saveMovement(a1, MovementType.OUT, 1);
        saveMovement(a1, MovementType.OUT, 1);
        flush();

        InventorySummaryResponse after = service.summary();

        assertThat(after.totalProducts() - before.totalProducts()).isEqualTo(3);
        assertThat(after.activeProducts() - before.activeProducts()).isEqualTo(2);
        assertThat(after.inactiveProducts() - before.inactiveProducts()).isEqualTo(1);
        assertThat(after.totalUnits() - before.totalUnits()).isEqualTo(17);
        assertThat(after.inventoryValue().subtract(before.inventoryValue())).isEqualByComparingTo("260.00");
        assertThat(after.criticalProducts() - before.criticalProducts()).isEqualTo(1);
        assertThat(after.totalMovements() - before.totalMovements()).isEqualTo(2);
    }

    @Test
    void topProducts_ordenaPorUnidadesOutYRespetaLimit() {
        Product a = saveProduct("top-a", ProductStatus.ACTIVE, "1.00", 100, 1);
        Product b = saveProduct("top-b", ProductStatus.ACTIVE, "1.00", 100, 1);
        Product c = saveProduct("top-c", ProductStatus.ACTIVE, "1.00", 100, 1);
        saveMovement(a, MovementType.OUT, 20);
        saveMovement(a, MovementType.OUT, 10); // a = 30 unidades, 2 movimientos
        saveMovement(b, MovementType.OUT, 50); // b = 50 unidades, 1 movimiento
        saveMovement(c, MovementType.OUT, 5);  // c = 5 unidades, 1 movimiento
        saveMovement(c, MovementType.IN, 100); // los IN no cuentan como salida
        flush();

        List<TopProductResponse> top2 = service.topProducts(2);

        assertThat(top2).hasSize(2);
        assertThat(top2.get(0).productId()).isEqualTo(b.getId());
        assertThat(top2.get(0).unitsOut()).isEqualTo(50);
        assertThat(top2.get(0).movementCount()).isEqualTo(1);
        assertThat(top2.get(1).productId()).isEqualTo(a.getId());
        assertThat(top2.get(1).unitsOut()).isEqualTo(30);
        assertThat(top2.get(1).movementCount()).isEqualTo(2);

        assertThat(service.topProducts(0)).hasSize(1);   // clamp a 1
        assertThat(service.topProducts(99)).hasSize(3);  // clamp a 50, solo hay 3
    }

    @Test
    void lowStock_incluyeIgualExcluyeSuperioresYOrdenaAscendente() {
        Product below = saveProduct("low-below", ProductStatus.ACTIVE, "1.00", 2, 10); // deficit 8
        Product equal = saveProduct("low-equal", ProductStatus.ACTIVE, "1.00", 5, 5);  // deficit 0, incluido (<=)
        Product above = saveProduct("low-above", ProductStatus.ACTIVE, "1.00", 20, 5); // excluido
        flush();

        List<LowStockProductResponse> low = service.lowStock();

        assertThat(low).extracting(LowStockProductResponse::productId)
                .contains(below.getId(), equal.getId())
                .doesNotContain(above.getId());

        List<Long> sembradosEnOrden = low.stream()
                .map(LowStockProductResponse::productId)
                .filter(id -> id.equals(below.getId()) || id.equals(equal.getId()))
                .toList();
        assertThat(sembradosEnOrden).containsExactly(below.getId(), equal.getId());

        LowStockProductResponse b = pick(low, below.getId());
        assertThat(b.quantity()).isEqualTo(2);
        assertThat(b.minimumStock()).isEqualTo(10);
        assertThat(b.deficit()).isEqualTo(8);
        assertThat(pick(low, equal.getId()).deficit()).isZero();
    }

    @Test
    void movementsByType_agrupaPorTipoYRespetaElRangoDeFechas() {
        Product p = saveProduct("mbt", ProductStatus.ACTIVE, "1.00", 100, 1);
        saveMovement(p, MovementType.IN, 5);
        saveMovement(p, MovementType.IN, 7);
        saveMovement(p, MovementType.OUT, 3);
        saveMovement(p, MovementType.ADJUSTMENT, 9);
        flush();

        Map<MovementType, MovementsByTypeResponse> byType = service.movementsByType(null, null).stream()
                .collect(Collectors.toMap(MovementsByTypeResponse::movementType, r -> r));

        assertThat(byType.get(MovementType.IN).movementCount()).isEqualTo(2);
        assertThat(byType.get(MovementType.IN).totalUnits()).isEqualTo(12);
        assertThat(byType.get(MovementType.OUT).movementCount()).isEqualTo(1);
        assertThat(byType.get(MovementType.OUT).totalUnits()).isEqualTo(3);
        assertThat(byType.get(MovementType.ADJUSTMENT).movementCount()).isEqualTo(1);
        assertThat(byType.get(MovementType.ADJUSTMENT).totalUnits()).isEqualTo(9);

        LocalDate today = LocalDate.now();
        assertThat(service.movementsByType(today, today)).isNotEmpty();               // hoy incluido
        assertThat(service.movementsByType(today.minusDays(2), today.minusDays(1))).isEmpty(); // pasado
        assertThat(service.movementsByType(today.plusDays(1), null)).isEmpty();       // futuro
    }

    @Test
    void movementsByType_conFromMayorQueTo_lanzaInvalidDateRangeException() {
        LocalDate today = LocalDate.now();

        assertThatThrownBy(() -> service.movementsByType(today, today.minusDays(1)))
                .isInstanceOf(InvalidDateRangeException.class);
    }

    private LowStockProductResponse pick(List<LowStockProductResponse> list, Long productId) {
        return list.stream().filter(r -> r.productId().equals(productId)).findFirst().orElseThrow();
    }

    private void flush() {
        em.flush();
    }

    private Product saveProduct(String token, ProductStatus status, String price, int quantity, int minimumStock) {
        Product p = new Product();
        p.setName("Producto " + token);
        p.setSku("SKU-" + token + "-" + System.nanoTime());
        p.setDescription("desc");
        p.setCategory("cat");
        p.setPrice(new BigDecimal(price));
        p.setQuantity(quantity);
        p.setMinimumStock(minimumStock);
        p.setStatus(status);
        return productRepository.save(p);
    }

    private StockMovement saveMovement(Product product, MovementType type, int quantity) {
        StockMovement m = new StockMovement();
        m.setProduct(product);
        m.setMovementType(type);
        m.setQuantity(quantity);
        m.setPreviousQuantity(0);
        m.setNewQuantity(quantity);
        m.setUserId("test");
        return stockMovementRepository.save(m);
    }
}
