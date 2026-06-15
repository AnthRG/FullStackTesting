package pucmm.freddy.fullstacktesting.dto;

import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductResponse(

        Long id,

        String name,

        String sku,

        String description,

        String category,

        BigDecimal price,

        Integer quantity,

        Integer minimumStock,

        ProductStatus status,

        LocalDateTime createdAt,

        LocalDateTime updatedAt

) {
    public static ProductResponse from (Product p){
        return new ProductResponse(
                p.getId(),
                p.getName(),
                p.getSku(),
                p.getDescription(),
                p.getCategory(),
                p.getPrice(),
                p.getQuantity(),
                p.getMinimumStock(),
                p.getStatus(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }
}
