package pucmm.freddy.fullstacktesting.dto;

import jakarta.validation.constraints.*;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;

import java.math.BigDecimal;

public record ProductRequest(

        @NotBlank
        String name,

        @NotBlank
        String sku,

        String description,

        @NotBlank
        String category,

        @NotNull @DecimalMin("0.0")
        BigDecimal price,

        @NotNull @Min(0)
        Integer quantity,

        @NotNull @Min(0)
        Integer minimumStock,

        @NotNull
        ProductStatus status
) {}
