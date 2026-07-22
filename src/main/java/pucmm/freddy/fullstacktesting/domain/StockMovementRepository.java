package pucmm.freddy.fullstacktesting.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pucmm.freddy.fullstacktesting.dto.MovementsByTypeResponse;
import pucmm.freddy.fullstacktesting.dto.TopProductResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long>, JpaSpecificationExecutor<StockMovement> {

    @Query("""
            select new pucmm.freddy.fullstacktesting.dto.TopProductResponse(
                p.id, p.name, p.sku, sum(m.quantity), count(m))
            from StockMovement m
            join m.product p
            where m.movementType = pucmm.freddy.fullstacktesting.domain.MovementType.OUT
            group by p.id, p.name, p.sku
            order by sum(m.quantity) desc, p.id asc""")
    List<TopProductResponse> findTopProductsByUnitsOut(Pageable pageable);

    @Query("""
            select new pucmm.freddy.fullstacktesting.dto.MovementsByTypeResponse(
                m.movementType, count(m), sum(m.quantity))
            from StockMovement m
            where (cast(:from as timestamp) is null or m.createdAt >= :from)
              and (cast(:to as timestamp) is null or m.createdAt < :to)
            group by m.movementType""")
    List<MovementsByTypeResponse> findMovementsByType(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

}
