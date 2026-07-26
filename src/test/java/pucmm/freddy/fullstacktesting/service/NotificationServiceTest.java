package pucmm.freddy.fullstacktesting.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import pucmm.freddy.fullstacktesting.domain.Notification;
import pucmm.freddy.fullstacktesting.domain.NotificationRepository;
import pucmm.freddy.fullstacktesting.domain.NotificationType;
import pucmm.freddy.fullstacktesting.domain.Product;
import pucmm.freddy.fullstacktesting.dto.NotificationListResponse;
import pucmm.freddy.fullstacktesting.exception.NotificationNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository repository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private NotificationService service;

    private Product product(int quantity, int minimumStock) {
        Product p = new Product();
        p.setId(1L);
        p.setName("Laptop");
        p.setSku("SKU-001");
        p.setQuantity(quantity);
        p.setMinimumStock(minimumStock);
        return p;
    }

    /** save() con IDENTITY asigna el id y @CreationTimestamp la fecha antes del commit. */
    private void stubSave() {
        when(repository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(7L);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });
    }

    private Notification savedNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }

    // ── evaluateStock: transiciones que SI alertan ───────────────────────────

    @Test
    void evaluateStock_cuandoCruzaElMinimo_generaAlertaLowStock() {
        stubSave();

        service.evaluateStock(product(2, 5), 10, 5);

        Notification alerta = savedNotification();
        assertThat(alerta.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(alerta.getQuantity()).isEqualTo(2);
        assertThat(alerta.getMinimumStock()).isEqualTo(5);
        assertThat(alerta.getProductName()).isEqualTo("Laptop");
        assertThat(alerta.getProductSku()).isEqualTo("SKU-001");
        assertThat(alerta.isRead()).isFalse();
        assertThat(alerta.getReadAt()).isNull();
        assertThat(alerta.getMessage()).contains("Stock bajo", "Laptop", "SKU-001");
    }

    @Test
    void evaluateStock_cuandoLlegaACeroDesdeStockSano_generaAlertaOutOfStock() {
        stubSave();

        service.evaluateStock(product(0, 5), 10, 5);

        assertThat(savedNotification().getType()).isEqualTo(NotificationType.OUT_OF_STOCK);
    }

    @Test
    void evaluateStock_cuandoLlegaACeroEstandoYaBajoMinimo_generaUnaSolaAlertaOutOfStock() {
        stubSave();

        service.evaluateStock(product(0, 5), 3, 5);

        Notification alerta = savedNotification();
        assertThat(alerta.getType()).isEqualTo(NotificationType.OUT_OF_STOCK);
        assertThat(alerta.getMessage()).contains("Sin stock");
        verify(repository, times(1)).save(any());
    }

    @Test
    void evaluateStock_alCrearProductoYaBajoMinimo_generaAlerta() {
        stubSave();

        service.evaluateStock(product(1, 5), null, null);

        assertThat(savedNotification().getType()).isEqualTo(NotificationType.LOW_STOCK);
    }

    @Test
    void evaluateStock_alCrearProductoSinStock_generaAlertaOutOfStock() {
        stubSave();

        service.evaluateStock(product(0, 5), null, null);

        assertThat(savedNotification().getType()).isEqualTo(NotificationType.OUT_OF_STOCK);
    }

    @Test
    void evaluateStock_cuandoElUpdateSubeElMinimoPorEncimaDeLaCantidad_generaAlerta() {
        stubSave();

        // La cantidad no cambia (10), pero el minimo pasa de 2 a 20: cruza el umbral.
        service.evaluateStock(product(10, 20), 10, 2);

        Notification alerta = savedNotification();
        assertThat(alerta.getType()).isEqualTo(NotificationType.LOW_STOCK);
        assertThat(alerta.getMinimumStock()).isEqualTo(20);
    }

    // ── evaluateStock: casos que NO alertan ──────────────────────────────────

    @Test
    void evaluateStock_cuandoSigueSobreElMinimo_noGeneraAlerta() {
        service.evaluateStock(product(8, 5), 10, 5);

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void evaluateStock_cuandoYaEstabaBajoMinimoYBajaMas_noGeneraAlertaRepetida() {
        service.evaluateStock(product(2, 5), 3, 5);

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void evaluateStock_cuandoYaEstabaEnCeroYSigueEnCero_noGeneraAlertaRepetida() {
        service.evaluateStock(product(0, 5), 0, 5);

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void evaluateStock_cuandoSeRecuperaPorEncimaDelMinimo_noGeneraAlerta() {
        service.evaluateStock(product(10, 5), 0, 5);

        verifyNoInteractions(repository, eventPublisher);
    }

    @Test
    void evaluateStock_alCrearProductoConStockSano_noGeneraAlerta() {
        service.evaluateStock(product(10, 5), null, null);

        verifyNoInteractions(repository, eventPublisher);
    }

    // ── evaluateStock: evento publicado ──────────────────────────────────────

    @Test
    void evaluateStock_publicaElEventoConElPayloadDeLaAlerta() {
        stubSave();

        service.evaluateStock(product(0, 5), 10, 5);

        ArgumentCaptor<NotificationCreatedEvent> captor =
                ArgumentCaptor.forClass(NotificationCreatedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        var payload = captor.getValue().payload();
        assertThat(payload.id()).isEqualTo(7L);
        assertThat(payload.type()).isEqualTo(NotificationType.OUT_OF_STOCK);
        assertThat(payload.productId()).isEqualTo(1L);
        assertThat(payload.productName()).isEqualTo("Laptop");
        assertThat(payload.productSku()).isEqualTo("SKU-001");
        assertThat(payload.quantity()).isZero();
        assertThat(payload.minimumStock()).isEqualTo(5);
        assertThat(payload.createdAt()).isNotNull();
        assertThat(payload.read()).isFalse();
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_sinFiltro_devuelveLasUltimasYElContadorDeNoLeidas() {
        Notification n = new Notification();
        n.setId(1L);
        n.setType(NotificationType.LOW_STOCK);
        n.setProduct(product(2, 5));
        n.setProductName("Laptop");
        n.setProductSku("SKU-001");
        n.setQuantity(2);
        n.setMinimumStock(5);
        n.setMessage("Stock bajo");
        when(repository.findLatest(any(Pageable.class))).thenReturn(List.of(n));
        when(repository.countByReadFalse()).thenReturn(3L);

        NotificationListResponse result = service.list(false);

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).productSku()).isEqualTo("SKU-001");
        assertThat(result.unreadCount()).isEqualTo(3L);
        verify(repository, never()).findLatestUnread(any());
    }

    @Test
    void list_conOnlyUnread_consultaSoloLasNoLeidas() {
        when(repository.findLatestUnread(any(Pageable.class))).thenReturn(List.of());
        when(repository.countByReadFalse()).thenReturn(0L);

        NotificationListResponse result = service.list(true);

        assertThat(result.items()).isEmpty();
        assertThat(result.unreadCount()).isZero();
        verify(repository, never()).findLatest(any());
    }

    @Test
    void list_pideComoMaximo50Elementos() {
        when(repository.findLatest(any(Pageable.class))).thenReturn(List.of());
        when(repository.countByReadFalse()).thenReturn(0L);

        service.list(false);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findLatest(captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(50);
    }

    // ── markRead / markAllRead / unreadCount ─────────────────────────────────

    @Test
    void markRead_conIdExistente_marcaLeidaYSellaLaFecha() {
        Notification n = new Notification();
        n.setId(1L);
        n.setRead(false);
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        service.markRead(1L);

        assertThat(n.isRead()).isTrue();
        assertThat(n.getReadAt()).isNotNull();
        verify(repository).save(n);
    }

    @Test
    void markRead_conNotificacionYaLeida_noVuelveAGuardar() {
        Notification n = new Notification();
        n.setId(1L);
        n.setRead(true);
        LocalDateTime original = LocalDateTime.now().minusDays(1);
        n.setReadAt(original);
        when(repository.findById(1L)).thenReturn(Optional.of(n));

        service.markRead(1L);

        assertThat(n.getReadAt()).isEqualTo(original);
        verify(repository, never()).save(any());
    }

    @Test
    void markRead_conIdInexistente_lanzaNotificationNotFoundException() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(99L))
                .isInstanceOf(NotificationNotFoundException.class);

        verify(repository, never()).save(any());
    }

    @Test
    void markAllRead_marcaTodasLasPendientes() {
        when(repository.markAllRead(any(LocalDateTime.class))).thenReturn(4);

        service.markAllRead();

        verify(repository).markAllRead(any(LocalDateTime.class));
    }

    @Test
    void unreadCount_devuelveElContadorDelRepositorio() {
        when(repository.countByReadFalse()).thenReturn(5L);

        assertThat(service.unreadCount()).isEqualTo(5L);
    }
}
