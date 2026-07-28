package pucmm.freddy.fullstacktesting.ws;

import lombok.RequiredArgsConstructor;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pucmm.freddy.fullstacktesting.dto.SocketMessage;
import pucmm.freddy.fullstacktesting.service.NotificationCreatedEvent;

/**
 * Puente entre el dominio y el transporte WebSocket.
 *
 * <p>Escucha en {@code AFTER_COMMIT} a propósito: si la transacción que registró el
 * movimiento hace rollback, la alerta no existe y tampoco se manda el push.</p>
 */
@Component
@RequiredArgsConstructor
public class NotificationBroadcaster {

    private final NotificationWebSocketHandler handler;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        handler.broadcast(SocketMessage.notification(event.payload()));
    }

    /** Keepalive: evita que proxies o el navegador corten una conexión ociosa. */
    @Scheduled(fixedDelayString = "${app.notifications.ping-interval-ms:30000}")
    public void ping() {
        handler.broadcast(SocketMessage.ping());
    }
}
