package pucmm.freddy.fullstacktesting.service;


import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pucmm.freddy.fullstacktesting.domain.ProductRepository;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;
import pucmm.freddy.fullstacktesting.domain.StockMovementRepository;
import pucmm.freddy.fullstacktesting.dto.InventorySummaryResponse;
import pucmm.freddy.fullstacktesting.dto.LowStockProductResponse;
import pucmm.freddy.fullstacktesting.dto.MovementsByTypeResponse;
import pucmm.freddy.fullstacktesting.dto.TopProductResponse;
import pucmm.freddy.fullstacktesting.exception.InvalidDateRangeException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor

public class ReportService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private final ProductRepository productRepository;
    private final StockMovementRepository stockMovementRepository;

    @Transactional(readOnly = true)
    public InventorySummaryResponse summary() {
        return new InventorySummaryResponse(
                productRepository.count(),
                productRepository.countByStatus(ProductStatus.ACTIVE),
                productRepository.countByStatus(ProductStatus.INACTIVE),
                productRepository.sumTotalUnits(),
                productRepository.sumInventoryValue(),
                productRepository.countCriticalProducts(),
                stockMovementRepository.count());
    }

    @Transactional(readOnly = true)
    public List<TopProductResponse> topProducts(int limit) {
        int safeLimit = Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, limit));
        return stockMovementRepository.findTopProductsByUnitsOut(PageRequest.of(0, safeLimit));
    }

    @Transactional(readOnly = true)
    public List<LowStockProductResponse> lowStock() {
        return productRepository.findLowStock();
    }

    @Transactional(readOnly = true)
    public List<MovementsByTypeResponse> movementsByType(LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new InvalidDateRangeException(from, to);
        }
        LocalDateTime fromDateTime = from != null ? from.atStartOfDay() : null;
        // El límite superior es exclusivo (inicio del día siguiente) para incluir todo el día 'to'.
        LocalDateTime toDateTime = to != null ? to.plusDays(1).atStartOfDay() : null;
        return stockMovementRepository.findMovementsByType(fromDateTime, toDateTime);
    }

}
