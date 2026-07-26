package pucmm.freddy.fullstacktesting.ws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import pucmm.freddy.fullstacktesting.domain.NotificationType;
import pucmm.freddy.fullstacktesting.dto.NotificationResponse;
import pucmm.freddy.fullstacktesting.dto.SocketMessage;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificationWebSocketHandlerTest {

    private NotificationWebSocketHandler handler;

    @BeforeEach
    void setUp() {
        handler = new NotificationWebSocketHandler(JsonMapper.builder().build());
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        lenient().when(session.getId()).thenReturn(id);
        lenient().when(session.isOpen()).thenReturn(true);
        return session;
    }

    private SocketMessage alerta() {
        return SocketMessage.notification(new NotificationResponse(
                1L, NotificationType.LOW_STOCK, 2L, "Laptop", "SKU-001", 1, 5,
                "Stock bajo", LocalDateTime.now(), false));
    }

    @Test
    void broadcast_enviaATodasLasSesionesAbiertas() throws IOException {
        WebSocketSession a = openSession("a");
        WebSocketSession b = openSession("b");
        handler.afterConnectionEstablished(a);
        handler.afterConnectionEstablished(b);

        handler.broadcast(alerta());

        assertThat(handler.openSessions()).isEqualTo(2);
        verify(a).sendMessage(any(TextMessage.class));
        verify(b).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcast_serializaElSobreConTypeYPayload() throws IOException {
        WebSocketSession session = openSession("a");
        handler.afterConnectionEstablished(session);

        handler.broadcast(alerta());

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        String json = captor.getValue().getPayload();
        assertThat(json).contains("\"type\":\"NOTIFICATION\"", "\"productSku\":\"SKU-001\"",
                "\"read\":false");
    }

    @Test
    void broadcast_conPing_omiteElPayloadNulo() throws IOException {
        WebSocketSession session = openSession("a");
        handler.afterConnectionEstablished(session);

        handler.broadcast(SocketMessage.ping());

        ArgumentCaptor<TextMessage> captor = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(captor.capture());
        assertThat(captor.getValue().getPayload()).isEqualTo("{\"type\":\"PING\"}");
    }

    @Test
    void broadcast_sinSesiones_noHaceNada() {
        handler.broadcast(alerta());

        assertThat(handler.openSessions()).isZero();
    }

    @Test
    void broadcast_conSesionCerrada_laDescartaYNoLeEnvia() throws IOException {
        WebSocketSession cerrada = openSession("cerrada");
        when(cerrada.isOpen()).thenReturn(false);
        WebSocketSession abierta = openSession("abierta");
        handler.afterConnectionEstablished(cerrada);
        handler.afterConnectionEstablished(abierta);

        handler.broadcast(alerta());

        verify(cerrada, never()).sendMessage(any());
        verify(abierta).sendMessage(any(TextMessage.class));
        assertThat(handler.openSessions()).isEqualTo(1);
    }

    @Test
    void broadcast_cuandoUnaSesionFalla_noPropagaYSigueConLasDemas() throws IOException {
        WebSocketSession rota = openSession("rota");
        doThrow(new IOException("broken pipe")).when(rota).sendMessage(any());
        WebSocketSession sana = openSession("sana");
        handler.afterConnectionEstablished(rota);
        handler.afterConnectionEstablished(sana);

        handler.broadcast(alerta());

        verify(sana).sendMessage(any(TextMessage.class));
        assertThat(handler.openSessions()).isEqualTo(1);
    }

    @Test
    void broadcast_cuandoLaSesionYaNoAceptaEnvios_laDescarta() throws IOException {
        WebSocketSession rota = openSession("rota");
        doThrow(new IllegalStateException("closed")).when(rota).sendMessage(any());
        handler.afterConnectionEstablished(rota);

        handler.broadcast(alerta());

        assertThat(handler.openSessions()).isZero();
    }

    @Test
    void broadcast_conUnMensajeNoSerializable_noPropagaNiEnvia() throws IOException {
        ObjectMapper roto = mock(ObjectMapper.class);
        when(roto.writeValueAsString(any())).thenThrow(new JacksonException("no serializable") {});
        NotificationWebSocketHandler conMapperRoto = new NotificationWebSocketHandler(roto);
        WebSocketSession session = openSession("a");
        conMapperRoto.afterConnectionEstablished(session);

        conMapperRoto.broadcast(alerta());

        verify(session, never()).sendMessage(any());
    }

    @Test
    void afterConnectionClosed_removeLaSesion() {
        WebSocketSession session = openSession("a");
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(handler.openSessions()).isZero();
    }

    @Test
    void handleTransportError_removeLaSesion() {
        WebSocketSession session = openSession("a");
        handler.afterConnectionEstablished(session);

        handler.handleTransportError(session, new IOException("reset"));

        assertThat(handler.openSessions()).isZero();
    }
}
