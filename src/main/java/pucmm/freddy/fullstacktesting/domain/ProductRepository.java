package pucmm.freddy.fullstacktesting.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import pucmm.freddy.fullstacktesting.dto.LowStockProductResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

        boolean existsBySku(String sku);

        boolean existsBySkuAndIdNot(String sku, Long id);

        // Bloquea la fila del producto mientras se registra un movimiento de stock,
        // para que dos movimientos concurrentes no lean la misma cantidad previa.
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from Product p where p.id = :id")
        Optional<Product> findByIdForUpdate(Long id);

        long countByStatus(ProductStatus status);

        @Query("select coalesce(sum(p.quantity), 0) from Product p")
        long sumTotalUnits();

        @Query("select coalesce(sum(p.price * p.quantity), 0) from Product p")
        BigDecimal sumInventoryValue();

        @Query("select count(p) from Product p where p.quantity <= p.minimumStock")
        long countCriticalProducts();

        @Query("""
                select new pucmm.freddy.fullstacktesting.dto.LowStockProductResponse(
                    p.id, p.name, p.sku, p.category, p.quantity, p.minimumStock,
                    p.minimumStock - p.quantity)
                from Product p
                where p.quantity <= p.minimumStock
                order by p.quantity asc""")
        List<LowStockProductResponse> findLowStock();

}
