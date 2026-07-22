package pucmm.freddy.fullstacktesting.dto;

import java.math.BigDecimal;

/** Resumen agregado del inventario para el tablero de reportes. */
public record InventorySummaryResponse(

        long totalProducts,

        long activeProducts,

        long inactiveProducts,

        long totalUnits,

        BigDecimal inventoryValue,

        long criticalProducts,

        long totalMovements

) {
}
