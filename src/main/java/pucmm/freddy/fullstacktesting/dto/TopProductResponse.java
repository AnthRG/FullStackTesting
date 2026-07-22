package pucmm.freddy.fullstacktesting.dto;

/** Producto con mayor salida de unidades, agregado sobre movimientos OUT. */
public record TopProductResponse(

        Long productId,

        String productName,

        String productSku,

        long unitsOut,

        long movementCount

) {
}
