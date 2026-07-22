package pucmm.freddy.fullstacktesting.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

        boolean existsBySku(String sku);

        boolean existsBySkuAndIdNot(String sku, Long id);

        // Bloquea la fila del producto mientras se registra un movimiento de stock,
        // para que dos movimientos concurrentes no lean la misma cantidad previa.
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("select p from Product p where p.id = :id")
        Optional<Product> findByIdForUpdate(Long id);

    
}
