package pucmm.freddy.fullstacktesting.dto;

/** Producto en o por debajo de su stock mínimo. */
public record LowStockProductResponse(

        Long productId,

        String name,

        String sku,

        String category,

        Integer quantity,

        Integer minimumStock,

        int deficit

) {
}
