package pucmm.freddy.fullstacktesting.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import pucmm.freddy.fullstacktesting.dto.InventorySummaryResponse;
import pucmm.freddy.fullstacktesting.dto.LowStockProductResponse;
import pucmm.freddy.fullstacktesting.dto.MovementsByTypeResponse;
import pucmm.freddy.fullstacktesting.dto.TopProductResponse;
import pucmm.freddy.fullstacktesting.service.ReportService;

import java.time.LocalDate;
import java.util.List;


@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes y métricas de solo lectura derivadas del inventario")

public class ReportController {

    private final ReportService service;

    @GetMapping("/summary")
    @Operation(summary = "Resumen del inventario",
            description = "Totales de productos (por estado), unidades, valor del inventario, "
                    + "productos críticos (stock <= mínimo) y cantidad de movimientos.")
    public InventorySummaryResponse summary() {
        return service.summary();
    }

    @GetMapping("/top-products")
    @Operation(summary = "Productos más movidos por salida",
            description = "Productos ordenados por unidades de salida (movimientos OUT) de mayor a menor. "
                    + "limit se acota al rango [1, 50] (por defecto 5).")
    public List<TopProductResponse> topProducts(@RequestParam(defaultValue = "5") int limit) {
        return service.topProducts(limit);
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Productos con stock bajo",
            description = "Productos cuya cantidad es menor o igual a su stock mínimo, "
                    + "ordenados por cantidad ascendente.")
    public List<LowStockProductResponse> lowStock() {
        return service.lowStock();
    }

    @GetMapping("/movements-by-type")
    @Operation(summary = "Movimientos agrupados por tipo",
            description = "Cantidad de movimientos y unidades por tipo. from/to son opcionales "
                    + "(fecha ISO yyyy-MM-dd) y filtran sobre createdAt; from > to responde 400.")
    public List<MovementsByTypeResponse> movementsByType(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.movementsByType(from, to);
    }

}
