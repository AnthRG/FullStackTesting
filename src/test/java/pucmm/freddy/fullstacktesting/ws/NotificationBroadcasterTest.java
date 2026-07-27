package pucmm.freddy.fullstacktesting.ws;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pucmm.freddy.fullstacktesting.domain.NotificationType;
import pucmm.freddy.fullstacktesting.dto.NotificationResponse;
import pucmm.freddy.fullstacktesting.dto.SocketMessage;
import pucmm.freddy.fullstacktesting.service.NotificationCreatedEvent;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationBroadcasterTest {

    @Mock
    private NotificationWebSocketHandler handler;

    @InjectMocks
    private NotificationBroadcaster broadcaster;

    @Test
    void onNotificationCreated_difundeElSobreDeTipoNotification() {
        NotificationResponse payload = new NotificationResponse(
                1L, NotificationType.OUT_OF_STOCK, 2L, "Laptop", "SKU-001", 0, 5,
                "Sin stock", LocalDateTime.now(), false);

        broadcaster.onNotificationCreated(new NotificationCreatedEvent(payload));

        ArgumentCaptor<SocketMessage> captor = ArgumentCaptor.forClass(SocketMessage.class);
        verify(handler).broadcast(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(SocketMessage.TYPE_NOTIFICATION);
        assertThat(captor.getValue().payload()).isEqualTo(payload);
    }

    @Test
    void ping_difundeElKeepalive() {
        broadcaster.ping();

        ArgumentCaptor<SocketMessage> captor = ArgumentCaptor.forClass(SocketMessage.class);
        verify(handler).broadcast(captor.capture());
        assertThat(captor.getValue().type()).isEqualTo(SocketMessage.TYPE_PING);
        assertThat(captor.getValue().payload()).isNull();
    }
}
