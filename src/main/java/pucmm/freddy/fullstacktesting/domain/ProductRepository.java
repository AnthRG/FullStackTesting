package pucmm.freddy.fullstacktesting.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
 
        boolean existsBySku(String sku);
        
        boolean existsBySkuAndIdNot(String sku, Long id);

    
}
