package pucmm.freddy.fullstacktesting.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pucmm.freddy.fullstacktesting.dto.NotificationListResponse;
import pucmm.freddy.fullstacktesting.service.NotificationService;


@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notificaciones", description = "Alertas de stock mínimo del inventario")

public class NotificationController {

    private final NotificationService service;

    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    @Operation(summary = "Lista las alertas de stock",
            description = "Últimas 50 alertas ordenadas por fecha descendente más el contador "
                    + "global de no leídas. onlyUnread=true devuelve solo las pendientes. "
                    + "Las alertas son globales del inventario, no por usuario.")
    public NotificationListResponse list(@RequestParam(defaultValue = "false") boolean onlyUnread) {
        return service.list(onlyUnread);
    }

    @PostMapping("/{id}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product:view')")
    @Operation(summary = "Marca una alerta como leída", description = "404 si la alerta no existe.")
    public void markRead(@PathVariable Long id) {
        service.markRead(id);
    }

    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('product:view')")
    @Operation(summary = "Marca todas las alertas como leídas")
    public void markAllRead() {
        service.markAllRead();
    }

}
