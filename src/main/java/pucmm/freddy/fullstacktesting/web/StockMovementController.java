package pucmm.freddy.fullstacktesting.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pucmm.freddy.fullstacktesting.domain.MovementType;
import pucmm.freddy.fullstacktesting.dto.StockMovementRequest;
import pucmm.freddy.fullstacktesting.dto.StockMovementResponse;
import pucmm.freddy.fullstacktesting.service.StockMovementService;


@RestController
@RequestMapping("/api/stock-movements")
@RequiredArgsConstructor
@Tag(name = "Movimientos de stock", description = "Registro y consulta de movimientos de inventario")

public class StockMovementController {

    private final StockMovementService service;

    @GetMapping
    @PreAuthorize("hasAuthority('stock:view')")
    public Page<StockMovementResponse> list(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) MovementType movementType,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return service.list(productId, movementType, pageable);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('stock:view')")
    public StockMovementResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('stock:manage')")
    @Operation(summary = "Registra un movimiento de stock",
            description = "IN suma al stock, OUT resta (falla con 409 si no hay suficiente) y "
                    + "ADJUSTMENT fija la cantidad exacta. Actualiza el producto y guarda el historial.")
    public StockMovementResponse register(@Valid @RequestBody StockMovementRequest req) {
        return service.register(req);
    }

}
