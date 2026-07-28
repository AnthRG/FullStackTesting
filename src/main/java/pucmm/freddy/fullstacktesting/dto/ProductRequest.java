package pucmm.freddy.fullstacktesting.dto;

import jakarta.validation.constraints.*;
import pucmm.freddy.fullstacktesting.domain.ProductStatus;

import java.math.BigDecimal;

// Los limites de longitud y precision replican los de las migraciones: sin ellos, un valor
// mas largo que la columna no se rechaza con 400 sino que llega a la base y revienta en 500.
public record ProductRequest(

        @NotBlank @Size(max = 150)
        String name,

        @NotBlank @Size(max = 50)
        String sku,

        String description,

        @NotBlank @Size(max = 50)
        String category,

        @NotNull @DecimalMin("0.0") @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @NotNull @Min(0)
        Integer quantity,

        @NotNull @Min(0)
        Integer minimumStock,

        @NotNull
        ProductStatus status
) {}
