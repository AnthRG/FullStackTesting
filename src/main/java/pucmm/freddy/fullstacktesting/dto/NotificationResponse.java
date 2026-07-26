package pucmm.freddy.fullstacktesting.dto;

import pucmm.freddy.fullstacktesting.domain.Notification;
import pucmm.freddy.fullstacktesting.domain.NotificationType;

import java.time.LocalDateTime;

/** Alerta de stock tal como la consume el frontend (REST y WebSocket). */
public record NotificationResponse(

        Long id,

        NotificationType type,

        Long productId,

        String productName,

        String productSku,

        Integer quantity,

        Integer minimumStock,

        String message,

        LocalDateTime createdAt,

        boolean read

) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getProduct().getId(),
                n.getProductName(),
                n.getProductSku(),
                n.getQuantity(),
                n.getMinimumStock(),
                n.getMessage(),
                n.getCreatedAt(),
                n.isRead()
        );
    }
}
