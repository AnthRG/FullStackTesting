package pucmm.freddy.fullstacktesting.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.dto.ProductAuditFeedItem;
import pucmm.freddy.fullstacktesting.dto.ProductRevisionResponse;
import pucmm.freddy.fullstacktesting.exception.ProductNotFoundException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class AuditServiceIT extends AbstractIntegrationTest {

    @Autowired
    private AuditService auditService;

    @Autowired
    private ProductRepository repository;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    void productRevisions_devuelveCreateYUpdateConSnapshotsEnOrdenAsc() {
        String sku = "AUDS-" + System.nanoTime();
        Long id = crearYEditar(sku);

        List<ProductRevisionResponse> revisiones = auditService.productRevisions(id);

        assertThat(revisiones).hasSize(2);

        ProductRevisionResponse create = revisiones.get(0);
        ProductRevisionResponse update = revisiones.get(1);

        assertThat(create.revisionType()).isEqualTo("CREATE");
        assertThat(update.revisionType()).isEqualTo("UPDATE");
        assertThat(create.revision()).isLessThan(update.revision());

        assertThat(create.username()).isEqualTo("system");
        assertThat(create.revisionDate()).isNotNull();

        assertThat(create.product().sku()).isEqualTo(sku);
        assertThat(create.product().price()).isEqualByComparingTo("10.00");
        assertThat(create.product().quantity()).isEqualTo(5);
        assertThat(create.product().status()).isEqualTo(ProductStatus.ACTIVE);

        assertThat(update.product().price()).isEqualByComparingTo("99.99");
        assertThat(update.product().quantity()).isEqualTo(50);
    }

    @Test
    void productFeed_paginaYordenaDescendentePorRevision() {
        String sku = "AUDF-" + System.nanoTime();
        Long id = crearYEditar(sku);

        Page<ProductAuditFeedItem> feed = auditService.productFeed(0, 10);

        assertThat(feed.getContent()).isNotEmpty();
        assertThat(feed.getSize()).isEqualTo(10);
        assertThat(feed.getNumber()).isZero();
        assertThat(feed.getTotalElements()).isGreaterThanOrEqualTo(2);

        assertThat(feed.getContent())
                .extracting(ProductAuditFeedItem::revision)
                .isSortedAccordingTo(Comparator.reverseOrder());

        ProductAuditFeedItem primero = feed.getContent().stream()
                .filter(i -> id.equals(i.productId()))
                .findFirst().orElseThrow();
        assertThat(primero.revisionType()).isEqualTo("UPDATE");
        assertThat(primero.productName()).isEqualTo("Producto auditado");
        assertThat(primero.productSku()).isEqualTo(sku);
    }

    @Test
    void productFeed_acotaElTamanoDePaginaAlRango() {
        crearYEditar("AUDC-" + System.nanoTime());

        assertThat(auditService.productFeed(0, 999).getSize()).isEqualTo(50);
        assertThat(auditService.productFeed(0, 0).getSize()).isEqualTo(1);
    }

    @Test
    void productRevisions_productoSinRevisiones_lanzaProductNotFoundException() {
        assertThatThrownBy(() -> auditService.productRevisions(999_999_999L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    private Long crearYEditar(String sku) {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Long id = tx.execute(s -> repository.save(nuevoProducto(sku)).getId());
        tx.executeWithoutResult(s -> {
            Product p = repository.findById(id).orElseThrow();
            p.setPrice(new BigDecimal("99.99"));
            p.setQuantity(50);
            repository.save(p);
        });
        return id;
    }

    private Product nuevoProducto(String sku) {
        Product p = new Product();
        p.setName("Producto auditado");
        p.setSku(sku);
        p.setDescription("creado por AuditServiceIT");
        p.setCategory("test");
        p.setPrice(new BigDecimal("10.00"));
        p.setQuantity(5);
        p.setMinimumStock(1);
        p.setStatus(ProductStatus.ACTIVE);
        return p;
    }
}
