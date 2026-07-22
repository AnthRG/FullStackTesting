package pucmm.freddy.fullstacktesting.dto;

import jakarta.validation.constraints.*;
import pucmm.freddy.fullstacktesting.domain.MovementType;

public record StockMovementRequest(

        @NotNull
        Long productId,

        @NotNull
        MovementType movementType,

        @NotNull @Positive
        Integer quantity,

        String observations
) {}
