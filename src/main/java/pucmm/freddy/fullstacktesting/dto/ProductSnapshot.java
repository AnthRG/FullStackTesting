package pucmm.freddy.fullstacktesting.dto;

import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductSnapshot(

        String name,

        String sku,

        String description,

        String category,

        BigDecimal price,

        Integer quantity,

        Integer minimumStock,

        ProductStatus status

) {
    public static ProductSnapshot from(Product p) {
        return new ProductSnapshot(
                p.getName(),
                p.getSku(),
                p.getDescription(),
                p.getCategory(),
                p.getPrice(),
                p.getQuantity(),
                p.getMinimumStock(),
                p.getStatus()
        );
    }
}
