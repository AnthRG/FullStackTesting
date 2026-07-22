package pucmm.freddy.fullstacktesting.dto;

import pucmm.freddy.fullstacktesting.domain.MovementType;

/** Conteo de movimientos y unidades agrupados por tipo de movimiento. */
public record MovementsByTypeResponse(

        MovementType movementType,

        long movementCount,

        long totalUnits

) {
}
