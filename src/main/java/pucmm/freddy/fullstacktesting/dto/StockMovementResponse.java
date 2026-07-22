package pucmm.freddy.fullstacktesting.dto;

import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.domain.StockMovement;

import java.time.LocalDateTime;

public record StockMovementResponse(

        Long id,

        Long productId,

        String productName,

        String productSku,

        MovementType movementType,

        Integer quantity,

        Integer previousQuantity,

        Integer newQuantity,

        String userId,

        String observations,

        LocalDateTime createdAt

) {
    public static StockMovementResponse from(StockMovement m) {
        return new StockMovementResponse(
                m.getId(),
                m.getProduct().getId(),
                m.getProduct().getName(),
                m.getProduct().getSku(),
                m.getMovementType(),
                m.getQuantity(),
                m.getPreviousQuantity(),
                m.getNewQuantity(),
                m.getUserId(),
                m.getObservations(),
                m.getCreatedAt()
        );
    }
}
