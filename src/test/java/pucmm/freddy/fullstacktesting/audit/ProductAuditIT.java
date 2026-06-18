package pucmm.freddy.fullstacktesting.audit;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class ProductAuditIT extends AbstractIntegrationTest {

    @Autowired
    private ProductRepository repository;

    @Autowired
    private PlatformTransactionManager txManager;

    @PersistenceContext
    private EntityManager em;

    @Test
    void generaRevisionesEnCreateUpdateYDelete() {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        String sku = "AUD-" + System.nanoTime();

        Long id = tx.execute(s -> repository.save(nuevoProducto(sku)).getId());

        tx.executeWithoutResult(s -> {
            Product p = repository.findById(id).orElseThrow();
            p.setPrice(new BigDecimal("99.99"));
            repository.save(p);
        });

        tx.executeWithoutResult(s -> repository.deleteById(id));

        tx.executeWithoutResult(s -> {
            AuditReader reader = AuditReaderFactory.get(em);

            @SuppressWarnings("unchecked")
            List<Object[]> historia = reader.createQuery()
                    .forRevisionsOfEntity(Product.class, false, true)
                    .add(AuditEntity.id().eq(id))
                    .getResultList();

            assertEquals(3, historia.size(), "deben existir 3 revisiones: create, update, delete");
            assertEquals(RevisionType.ADD, historia.get(0)[2], "revision 1 = INSERT");
            assertEquals(RevisionType.MOD, historia.get(1)[2], "revision 2 = UPDATE");
            assertEquals(RevisionType.DEL, historia.get(2)[2], "revision 3 = DELETE");

            Product creado = (Product) historia.get(0)[0];
            assertTrue(new BigDecimal("10.00").compareTo(creado.getPrice()) == 0,
                    "la revision de creacion conserva el precio original");

            Product editado = (Product) historia.get(1)[0];
            assertTrue(new BigDecimal("99.99").compareTo(editado.getPrice()) == 0,
                    "la revision de edicion refleja el precio nuevo");

            assertEquals("system", creado.getCreatedBy(), "created_by se pobla en el INSERT");
            assertEquals("system", editado.getUpdatedBy(), "updated_by se pobla en el UPDATE");
        });
    }

    private Product nuevoProducto(String sku) {
        Product p = new Product();
        p.setName("Producto auditado");
        p.setSku(sku);
        p.setDescription("creado por el test de auditoria");
        p.setCategory("test");
        p.setPrice(new BigDecimal("10.00"));
        p.setQuantity(5);
        p.setMinimumStock(1);
        p.setStatus(ProductStatus.ACTIVE);
        return p;
    }
}
