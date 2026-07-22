package pucmm.freddy.fullstacktesting.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import pucmm.freddy.fullstacktesting.dto.ProductAuditFeedItem;
import pucmm.freddy.fullstacktesting.dto.ProductRevisionResponse;
import pucmm.freddy.fullstacktesting.service.AuditService;

import java.util.List;


@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@Tag(name = "Auditoría", description = "Historial de cambios de productos registrado por Hibernate Envers")

public class AuditController {

    private final AuditService service;

    @GetMapping("/products/{id}/revisions")
    @Operation(summary = "Historial de revisiones de un producto",
            description = "Lista todas las revisiones (CREATE, UPDATE, DELETE) de un producto en orden ascendente por "
                    + "número de revisión. Cada elemento incluye el snapshot del producto en esa revisión "
                    + "(en un DELETE trae el ultimo estado conocido, por store_data_at_delete). Responde 404 si el producto no tiene ninguna revisión.")
    public List<ProductRevisionResponse> productRevisions(@PathVariable Long id) {
        return service.productRevisions(id);
    }

    @GetMapping("/products")
    @Operation(summary = "Feed global de revisiones de productos",
            description = "Revisiones de todos los productos en orden descendente por número de revisión, paginadas. "
                    + "size se acota al rango [1, 50] (por defecto 10) y page es >= 0.")
    public Page<ProductAuditFeedItem> productFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return service.productFeed(page, size);
    }

}
