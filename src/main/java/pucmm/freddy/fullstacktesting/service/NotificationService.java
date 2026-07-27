package pucmm.freddy.fullstacktesting.service;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pucmm.freddy.fullstacktesting.domain.Notification;
import pucmm.freddy.fullstacktesting.domain.NotificationRepository;
import pucmm.freddy.fullstacktesting.domain.NotificationType;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.dto.NotificationListResponse;
import pucmm.freddy.fullstacktesting.dto.NotificationResponse;
import pucmm.freddy.fullstacktesting.exception.NotificationNotFoundException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * Alertas de stock del inventario.
 *
 * <p>Las notificaciones son <b>globales</b>, no por usuario: cualquiera con
 * {@code product:view} ve las mismas alertas y el estado leído/no leído es
 * compartido por todo el equipo.</p>
 */
@Service
@RequiredArgsConstructor

public class NotificationService {

    private static final int MAX_ITEMS = 50;

    private final NotificationRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Genera la alerta correspondiente si el producto <b>cruzó</b> un umbral con este
     * cambio. Solo se alerta en la transición, nunca en cada movimiento: si el producto
     * ya venía bajo mínimo y baja más, no se emite nada. Un estado previo nulo
     * (producto recién creado) cuenta como "venía sano", así que un producto que nace
     * bajo mínimo sí alerta.
     *
     * <p>La alerta se persiste dentro de la transacción del llamador; el push por
     * WebSocket lo dispara el evento, ya después del commit.</p>
     */
    @Transactional
    public void evaluateStock(Product product, Integer previousQuantity, Integer previousMinimum) {
        int quantity = product.getQuantity();
        int minimum = product.getMinimumStock();

        boolean wasOut = previousQuantity != null && previousQuantity == 0;
        boolean wasLow = previousQuantity != null && previousMinimum != null
                && previousQuantity <= previousMinimum;

        NotificationType type;
        if (quantity == 0 && !wasOut) {
            // Llegar a 0 gana sobre LOW_STOCK: una sola alerta por transición a cero,
            // venga de stock sano o de un producto que ya estaba bajo mínimo.
            type = NotificationType.OUT_OF_STOCK;
        } else if (quantity <= minimum && !wasLow && quantity > 0) {
            type = NotificationType.LOW_STOCK;
        } else {
            return;
        }

        Notification saved = repository.save(build(product, type, quantity, minimum));
        eventPublisher.publishEvent(new NotificationCreatedEvent(NotificationResponse.from(saved)));
    }

    @Transactional(readOnly = true)
    public NotificationListResponse list(boolean onlyUnread) {
        Pageable limit = PageRequest.of(0, MAX_ITEMS);
        List<Notification> found = onlyUnread
                ? repository.findLatestUnread(limit)
                : repository.findLatest(limit);
        return new NotificationListResponse(
                found.stream().map(NotificationResponse::from).toList(),
                repository.countByReadFalse());
    }

    @Transactional
    public void markRead(Long id) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now(ZoneId.systemDefault()));
            repository.save(notification);
        }
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead(LocalDateTime.now(ZoneId.systemDefault()));
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByReadFalse();
    }

    private Notification build(Product product, NotificationType type, int quantity, int minimum) {
        Notification notification = new Notification();
        notification.setType(type);
        notification.setProduct(product);
        notification.setProductName(product.getName());
        notification.setProductSku(product.getSku());
        notification.setQuantity(quantity);
        notification.setMinimumStock(minimum);
        notification.setMessage(message(type, product, quantity, minimum));
        notification.setRead(false);
        return notification;
    }

    private String message(NotificationType type, Product product, int quantity, int minimum) {
        return type == NotificationType.OUT_OF_STOCK
                ? "Sin stock: %s (%s) se quedó en 0 unidades".formatted(product.getName(), product.getSku())
                : "Stock bajo: %s (%s) tiene %d unidades y el mínimo es %d"
                        .formatted(product.getName(), product.getSku(), quantity, minimum);
    }
}
